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

import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.concat;
import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.empty;
import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.filter;
import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.map;
import static java.util.Spliterator.DISTINCT;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterators.spliteratorUnknownSize;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.List;
import java.util.PrimitiveIterator.OfLong;
import java.util.function.LongFunction;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.github.jervenbolleman.handlegraph4j.NodeSequence;
import io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator;
import io.github.jervenbolleman.handlegraph4j.iterators.CollectingOfLong;
import io.github.jervenbolleman.handlegraph4j.sequences.Sequence;
import io.github.jervenbolleman.handlegraph4j.sequences.SequenceType;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.sequences.BufferedLongSequenceMap;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.sequences.BufferedShortSequenceMap;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.sequences.MediumSequenceMap;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.sequences.NodeSequenceMap;

/**
 *
 @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 */
public class BufferedNodeToSequenceMap implements NodeToSequenceMap {

	private final BufferedShortSequenceMap nodesWithShortSequences;
	private final NodeSequenceMap nodesWithMediumSequences;
	private final NodeSequenceMap nodesWithLongSequences;

	private long maxNodeId;

	public BufferedNodeToSequenceMap(RandomAccessFile raf) throws IOException {
		this.nodesWithShortSequences = new BufferedShortSequenceMap(raf);
		this.nodesWithMediumSequences = MediumSequenceMap.open(raf);
		this.nodesWithLongSequences = new BufferedLongSequenceMap(raf);
	}

	@Override
	public Stream<SimpleNodeHandle> nodes() {
		return nodesIds().mapToObj(SimpleNodeHandle::new);
	}

	@Override
	public Iterator<SimpleNodeHandle> nodeIterator() {
		OfLong ids = nodeIdsIterator();
		return new Iterator<SimpleNodeHandle>() {
			@Override
			public boolean hasNext() {
				return ids.hasNext();
			}

			@Override
			public SimpleNodeHandle next() {
				long id = ids.nextLong();
				return new SimpleNodeHandle(id);
			}
		};
	}

	@Override
	public Iterator<NodeSequence<SimpleNodeHandle>> nodeWithSequenceIterator() {
		var smalls = nodesWithShortSequences.nodeSequences();

		var longs = nodesWithLongSequences.nodeSequences();
		var mediums = nodesWithMediumSequences.nodeSequences();
		var iters = concat(concat(longs, mediums), smalls);
		return iters;
	}

	@Override
	public void writeToDisk(DataOutputStream raf) {
//        nodesWithShortSequences.w
	}

	public static class AttachNodeToSequence implements LongFunction<NodeSequence<SimpleNodeHandle>> {

		private final Sequence seq;

		public AttachNodeToSequence(Sequence seq) {
			this.seq = seq;
		}

		@Override
		public NodeSequence<SimpleNodeHandle> apply(long id) {
			var node = new SimpleNodeHandle(id);
			return new NodeSequence<>(node, seq);
		}
	}

	@Override
	public AutoClosedIterator<SimpleNodeHandle> nodesWithSequence(Sequence s) {
		if (s == null) {
			return empty();
		}
		if (nodesWithShortSequences.containsSequence(s)) {
			return nodesWithShortSequences.nodeWithSequences(s);
		}
		if (s.getType() == SequenceType.LONG_VIA_ID) {
			var nAndSeq = nodesWithLongSequences.nodeSequences();
			var filter = filter(nAndSeq, ns -> s.equals(ns.sequence()));
			return map(filter, NodeSequence::node);
		} else {
			return nodesWithMediumSequences.nodeWithSequences(s);
		}
	}

	@Override
	public Sequence getSequence(SimpleNodeHandle handle) {
		long postiveId = Math.abs(handle.id());
		Sequence seq = nodesWithShortSequences.getSequence(postiveId);
		if (seq != null) {
			if (postiveId == handle.id())
				return seq;
			else
				return seq.reverseComplement();
		}
		seq = nodesWithLongSequences.getSequence(postiveId);
		if (seq != null) {
			if (postiveId == handle.id())
				return seq;
			else
				return seq.reverseComplement();
		}
		seq = nodesWithMediumSequences.getSequence(postiveId);
		if (seq != null) {
			if (postiveId == handle.id())
				return seq;
			else
				return seq.reverseComplement();
		}

		throw new IllegalStateException(
				"A simple node handle was passed in which does not have a sequence:" + handle.id());
	}

	@Override
	public int getSequenceLength(SimpleNodeHandle handle) {
		long postiveId = Math.abs(handle.id());
		int seqL = nodesWithShortSequences.getSequenceLength(postiveId);
		if (seqL > 0) {
			return seqL;
		} else {
			seqL = nodesWithLongSequences.getSequenceLength(postiveId);
			if (seqL > 0) {
				return seqL;
			}
			seqL = nodesWithMediumSequences.getSequenceLength(postiveId);
			if (seqL > 0) {
				return seqL;
			}

			throw new IllegalStateException(
					"A simple node handle was passed in which does not have a sequence:" + handle.id());
		}
	}

	public void add(long id, Sequence sequence) {
		maxNodeId = Math.max(maxNodeId, id);
		if (nodesWithShortSequences.containsSequence(sequence)) {
			nodesWithShortSequences.add(id, sequence);
		} else if (sequence.getType() == SequenceType.SHORT_KNOWN
				|| sequence.getType() == SequenceType.SHORT_AMBIGUOUS) {

			nodesWithMediumSequences.add(id, sequence);
		} else {
			nodesWithLongSequences.add(id, sequence);
		}
	}

	public void trim() {
		nodesWithShortSequences.trim();
		nodesWithLongSequences.trim();
		nodesWithMediumSequences.trim();
	}

	@Override
	public long getMaxNodeId() {
		return maxNodeId;
	}

	@Override
	public boolean areAllSequencesOneBaseLong() {

		if (nodesWithMediumSequences.isEmpty() && nodesWithLongSequences.isEmpty()) {
			return nodesWithShortSequences.maxSequenceLength() == 1;
		}
		return false;
	}

	@Override
	public LongStream nodesIds() {
		return streamFromOfLong(nodeIdsIterator());
	}

	@Override
	public OfLong nodeIdsIterator() {
		List<OfLong> li = List.of(nodesWithShortSequences.nodeIds(), nodesWithMediumSequences.nodeIds(),
				nodesWithLongSequences.nodeIds());
		return new CollectingOfLong(li.iterator());
	}

	private LongStream streamFromOfLong(OfLong iterator) {
		var si = spliteratorUnknownSize(iterator, DISTINCT | NONNULL);
		return StreamSupport.longStream(si, false);
	}

	@Override
	public long count() {
		long shortSeqs = nodesWithShortSequences.size();
		long mediumSeqs = nodesWithMediumSequences.size();
		long longSeqs = nodesWithLongSequences.size();
		return shortSeqs + mediumSeqs + longSeqs;
	}
}
