/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import swiss.sib.swissprot.handlegraph4j.simple.functions.LongLongToObj;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.chunks.BasicChunk;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.chunks.CompressedChunk;
import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.from;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.filter;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.map;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.flatMap;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.empty;
import io.github.vgteam.handlegraph4j.iterators.CollectingOfLong;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import static java.nio.channels.FileChannel.MapMode.READ_ONLY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PrimitiveIterator.OfLong;
import java.util.function.Predicate;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.chunks.BasicBufferedChunk;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.chunks.Chunk;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.chunks.Chunk.Type;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.chunks.CompressedBufferedChunk;
import swiss.sib.swissprot.handlegraph4j.simple.functions.ToLong;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 * @param <T> the type of object represented by this key value store
 */
public class LongLongSpinalList<T> {

    public static final int CHUNK_SIZE = 32 * 1024;
    private final LongLongToObj<T> reconstructor;
    private final ToLong<T> getKey;
    private final ToLong<T> getValue;
    private final Comparator<T> comparator;
    private final ArrayList<Chunk<T>> chunks = new ArrayList<>();
    private BasicChunk<T> current;

    public LongLongSpinalList(LongLongToObj<T> reconstructor,
            ToLong<T> getKey,
            ToLong<T> getValue,
            Comparator<T> comparator) {
        this.reconstructor = reconstructor;
        this.getKey = getKey;
        this.getValue = getValue;
        this.comparator = comparator;
    }

    public void toStream(OutputStream stream) throws IOException {
        for (Chunk<T> chunk : chunks) {
            stream.write(chunk.getType().getCode());
            chunk.toStream(stream);
        }
    }

    public void fromStream(RandomAccessFile raf) throws IOException {
        long max = raf.length() - 5;
        int read = 0;
        while (read < max) {
            byte type = raf.readByte();
            read++;
            int size = raf.readInt();
            read += Integer.BYTES;
            MappedByteBuffer map = raf.getChannel().map(READ_ONLY, read, size);
            read+=size;
            raf.seek(read);
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
            }

        }
    }

    public Iterator<T> iterator() {
        Iterator<Chunk<T>> iterator = chunks
                .iterator();
        AutoClosedIterator<Chunk<T>> as = from(iterator);
        var mapped = map(as, Chunk::iterator);
        var toFlatMap = map(mapped, AutoClosedIterator::from);
        return flatMap(toFlatMap);
    }

    public void trimAndSort() {
        if (current != null) {
            current.sort();
            chunks.add(current);
            current = null;
        }
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
        return flatMap(map(chunksHavingKey, c -> c.<T>fromKey(key, getKey)));
    }

    private int findFirstChunkThatMightMatch(long key) {
        int index = Collections.binarySearch(chunks,
                new SearchChunk<>(key),
                (l, r) -> Long.compare(l.firstKey(), r.firstKey()));
        if (index > 0) {
            //We might need to backtrack as we found a chunck in which
            //have the key, but chunks that are before this one might have the
            //key as well
            while (index > 0 && chunks.get(index - 1).firstKey() == key) {
                index--;
            }
        } else if (index < 0) {
            index = Math.abs(index + 1);
            //If the insertion spot is after the last chunk the 
            //content can only be in the last chunk
            if (index >= chunks.size()) {
                return chunks.size() - 1;
            }
            //Make sure that we do not need to go to an earlier chunk
            while (index > 0 && chunks.get(index).firstKey() > key) {
                index--;
            }
        }
        return index;
    }

    private int lastFirstChunkThatMightMatch(int index, long key) {
        index++;
        //Check if there are more chunks that might have the data
        while (index < chunks.size() - 1
                && chunks.get(index + 1).lastKey() <= key) {
            index++;
        }
        return index;
    }

    void sort() {
        for (Chunk<T> c : chunks) {
            c.sort();
        }
        chunks.sort((l, r)
                -> Long.compare(
                        getKey.apply(l.first()),
                        getKey.apply(r.first())
                ));
    }

    public void add(T t) {

        if (current == null) {
            current = new BasicChunk<>(reconstructor,
                    getKey,
                    getValue,
                    comparator);
        } else if (current.isFull()) {
            current.sort();
            if (CompressedChunk.<T>canCompress(current, getKey, getValue)) {
                CompressedChunk<T> cc = new CompressedChunk<>(current,
                        reconstructor,
                        getKey,
                        getValue);
                chunks.add(cc);
            } else {
                chunks.add(current);
            }
            current = new BasicChunk<>(reconstructor,
                    getKey,
                    getValue,
                    comparator);
        }
        current.add(t);
    }

    boolean isEmpty() {
        if (chunks.isEmpty() && current == null) {
            return true;
        } else if (current != null) {
            return current.size() == 0;
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
        Iterator<OfLong> iter = chunks
                .stream()
                .map(Chunk::keyIterator)
                .iterator();
        return new CollectingOfLong(iter);
    }

    public OfLong valueIterator() {
        Iterator<OfLong> iter = chunks
                .stream()
                .map(Chunk::valueIterator)
                .iterator();
        return new CollectingOfLong(iter);
    }

    public long size() {
        return chunks.stream()
                .mapToLong(c -> c.size())
                .sum();
    }

    AutoClosedIterator<T> iterateWithValue(long id) {
        Predicate<T> p = e -> getValue.apply(e) == id;
        AutoClosedIterator<T> from = from(iterator());
        return filter(from, p);

    }

    private static class SearchChunk<T> implements Chunk<T> {

        private static final String NOT_SUPPORTED
                = "Not supported, this is just for the binary search.";
        private final long key;

        public SearchChunk(long key) {
            this.key = key;
        }

        @Override
        public long size() {
            throw new UnsupportedOperationException(NOT_SUPPORTED);
        }

        @Override
        public boolean isFull() {
            throw new UnsupportedOperationException(NOT_SUPPORTED);
        }

        @Override
        public void sort() {
            throw new UnsupportedOperationException(NOT_SUPPORTED);
        }

        @Override
        public T first() {
            throw new UnsupportedOperationException(NOT_SUPPORTED);
        }

        @Override
        public T last() {
            throw new UnsupportedOperationException(NOT_SUPPORTED);
        }

        @Override
        public OfLong keyIterator() {
            throw new UnsupportedOperationException(NOT_SUPPORTED);
        }

        @Override
        public OfLong valueIterator() {
            throw new UnsupportedOperationException(NOT_SUPPORTED);
        }

        @Override
        public Iterator<T> iterator(int start) {
            throw new UnsupportedOperationException(NOT_SUPPORTED);
        }

        @Override
        public long firstKey() {
            return key;
        }

        @Override
        public long lastKey() {
            throw new UnsupportedOperationException(NOT_SUPPORTED);
        }

        @Override
        public void toStream(OutputStream stream) throws IOException {
            throw new UnsupportedOperationException(NOT_SUPPORTED);
        }

        @Override
        public Type getType() {
            return Type.SORT;
        }
    }
}
