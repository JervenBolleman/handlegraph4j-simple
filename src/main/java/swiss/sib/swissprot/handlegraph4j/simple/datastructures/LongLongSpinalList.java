/**
 * Copyright (c) 2020, SIB Swiss Institute of Bioinformatics
 * and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.empty;
import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.filter;
import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.flatMap;
import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.from;
import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.map;
import static java.nio.channels.FileChannel.MapMode.READ_ONLY;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PrimitiveIterator.OfLong;
import java.util.function.Predicate;

import io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator;
import io.github.jervenbolleman.handlegraph4j.iterators.CollectingOfLong;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.chunks.BasicBufferedChunk;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.chunks.BasicChunk;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.chunks.Chunk;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.chunks.Chunk.Type;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.chunks.CompressedBufferedChunk;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.chunks.CompressedChunk;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.chunks.SearchChunk;
import swiss.sib.swissprot.handlegraph4j.simple.functions.LongLongToObj;
import swiss.sib.swissprot.handlegraph4j.simple.functions.ToLong;

/**
 *
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 * @param <T> the type of object represented by this key value store
 */
public class LongLongSpinalList<T> {

	public static final int CHUNK_SIZE = 32 * 1024;
	private final LongLongToObj<T> reconstructor;
	private final ToLong<T> getKey;
	private final ToLong<T> getValue;
	private final Comparator<T> comparator;
	private final ArrayList<Chunk<T>> chunks = new ArrayList<>();

	public LongLongSpinalList(LongLongToObj<T> reconstructor, ToLong<T> getKey, ToLong<T> getValue,
			Comparator<T> comparator) {
		this.reconstructor = reconstructor;
		this.getKey = getKey;
		this.getValue = getValue;
		this.comparator = comparator;
	}

	public void toStream(DataOutputStream stream) throws IOException {
		stream.writeInt(chunks.size());
		for (Chunk<T> chunk : chunks) {
			stream.write(chunk.getType().getCode());
			chunk.toStream(stream);
		}
	}

	public void fromStream(RandomAccessFile raf) throws IOException {
		int noOfChunks = raf.readInt();
		while (chunks.size() < noOfChunks) {
			byte type = raf.readByte();
			int size = raf.readInt();
			MappedByteBuffer map = raf.getChannel().map(READ_ONLY, raf.getFilePointer(), size);
			raf.seek(raf.getFilePointer() + (long) size);
			Type fromCode = Type.fromCode(type);
			switch (fromCode) {
			case BASIC:
			case BASIC_BUFFERED:
				chunks.add(new BasicBufferedChunk<>(map, reconstructor));
				break;
			case COMPRESSED:
			case COMPRESSED_BUFFERED:
				chunks.add(new CompressedBufferedChunk<>(map, reconstructor, getKey));
				break;
			case SORT:
				throw new RuntimeException("Sort is a special chunk that should never be written to disk.");
			}
		}
	}

	public Iterator<T> iterator() {
		Iterator<Chunk<T>> iterator = chunks.iterator();
		AutoClosedIterator<Chunk<T>> as = from(iterator);
		var mapped = map(as, Chunk::iterator);
		var toFlatMap = map(mapped, AutoClosedIterator::from);
		return flatMap(toFlatMap);
	}

	public void trimAndSort() {
		sort();
	}

	public AutoClosedIterator<T> iterateWithKey(long key) {
		if (chunks.isEmpty()) {
			return empty();
		}
		int firstIndex = 0;
		int lastIndex = 1;
		if (chunks.size() > 1) {
			firstIndex = findFirstChunkThatMightMatch(key);
			lastIndex = lastFirstChunkThatMightMatch(firstIndex, key);
		}
		List<Chunk<T>> potential = chunks.subList(firstIndex, lastIndex);
		if (potential.isEmpty()) {
			return empty();
		}
		AutoClosedIterator<Chunk<T>> from = from(potential.iterator());

		AutoClosedIterator<Chunk<T>> chunksHavingKey = filter(from, c -> c.hasKey(key));
		return flatMap(map(chunksHavingKey, c -> c.fromKey(key, getKey)));
	}

	private int findFirstChunkThatMightMatch(long key) {
		int index = Collections.binarySearch(chunks, new SearchChunk<>(key),
				(l, r) -> Long.compare(l.firstKey(), r.firstKey()));
		if (index > 0) {
			// We might need to backtrack as we found a chunck in which
			// have the key, but chunks that are before this one might have the
			// key as well
			while (index > 0 && chunks.get(index - 1).firstKey() == key) {
				index--;
			}
		} else if (index < 0) {
			index = Math.abs(index + 1);
			// If the insertion spot is after the last chunk the
			// content can only be in the last chunk
			if (index >= chunks.size()) {
				return chunks.size() - 1;
			}
			// Make sure that we do not need to go to an earlier chunk
			while (index > 0 && chunks.get(index).firstKey() > key) {
				index--;
			}
		}
		return index;
	}

	private int lastFirstChunkThatMightMatch(int index, long key) {
		index++;
		// Check if there are more chunks that might have the data
		while (index < chunks.size() - 1 && chunks.get(index + 1).lastKey() <= key) {
			index++;
		}
		return index;
	}

	void sort() {
		for (Chunk<T> c : chunks) {
			c.sort();
		}
		chunks.sort((l, r) -> Long.compare(getKey.apply(l.first()), getKey.apply(r.first())));
	}

	public void add(T t) {
		Chunk<T> current;
		if (chunks.isEmpty()) {
			addToNewChunk(t);
		} else {
			current = chunks.get(chunks.size() - 1);

			if (current instanceof BasicChunk<T> bc) {
				if (bc.isFull()) {
					bc.sort();

					if (CompressedChunk.<T>canCompress(bc, getKey, getValue)) {
						CompressedChunk<T> cc = new CompressedChunk<>(bc, reconstructor, getKey, getValue);
						chunks.remove(chunks.size() - 1);
						chunks.add(cc);
					}
					addToNewChunk(t);
				} else {
					bc.add(t);
				}
			} else {
				addToNewChunk(t);
			}
		}
	}

	private void addToNewChunk(T t) {
		BasicChunk<T> bc = new BasicChunk<>(reconstructor, getKey, getValue, comparator);
		bc.add(t);
		chunks.add(bc);
	}

	boolean isEmpty() {
		if (chunks.isEmpty()) {
			return true;
		} else {
			for (Chunk<T> c : chunks) {
				if (c.first() != null) {
					return false;
				}
			}
		}
		return true;
	}

	public OfLong keyIterator() {
		Iterator<OfLong> iter = chunks.stream().map(Chunk::keyIterator).iterator();
		return new CollectingOfLong(iter);
	}

	public OfLong valueIterator() {
		Iterator<OfLong> iter = chunks.stream().map(Chunk::valueIterator).iterator();
		return new CollectingOfLong(iter);
	}

	public long size() {
		return chunks.stream().mapToLong(c -> c.size()).sum();
	}

	AutoClosedIterator<T> iterateWithValue(long id) {
		Predicate<T> p = e -> getValue.apply(e) == id;
		AutoClosedIterator<T> from = from(iterator());
		return filter(from, p);

	}

}
