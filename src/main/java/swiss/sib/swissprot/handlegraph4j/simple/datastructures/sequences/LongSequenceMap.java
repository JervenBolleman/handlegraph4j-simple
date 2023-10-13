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
package swiss.sib.swissprot.handlegraph4j.simple.datastructures.sequences;

import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.filter;
import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.from;
import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.map;

import java.util.Iterator;
import java.util.PrimitiveIterator.OfLong;

import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.LongIntHashMap;

import io.github.jervenbolleman.handlegraph4j.NodeSequence;
import io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator;
import io.github.jervenbolleman.handlegraph4j.sequences.LongSequence;
import io.github.jervenbolleman.handlegraph4j.sequences.Sequence;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;

/**
 *
 @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 */
public class LongSequenceMap implements NodeSequenceMap {

	final LongIntHashMap nodesWithLongSequences;
	final LongArrayList longSequenceLinearLayout;

	public LongSequenceMap(LongIntHashMap nodesWithLongSequences, LongArrayList longSequenceLinearLayout) {
		this.nodesWithLongSequences = nodesWithLongSequences;
		this.longSequenceLinearLayout = longSequenceLinearLayout;
	}

	public LongSequenceMap() {

		this.nodesWithLongSequences = new LongIntHashMap();
		this.longSequenceLinearLayout = new LongArrayList();
	}

	@Override
	public AutoClosedIterator<NodeSequence<SimpleNodeHandle>> nodeSequences() {
		return from(new LinearLongSequenceIterator(longSequenceLinearLayout));
	}

	@Override
	public Sequence getSequence(long id) {
		int offset = nodesWithLongSequences.getIfAbsent(id, Integer.MAX_VALUE);
		if (Integer.MAX_VALUE == offset) {
			return null;
		} else {
			return getLongSequence(offset);
		}
	}

	private LongSequence getLongSequence(int offset) {
//        long nodeid = longSequenceLinearLayout.get(offset);
		long sizeAndLongs = longSequenceLinearLayout.get(offset + 1);
		int size = (int) (sizeAndLongs >>> 32);
		int longs = (int) sizeAndLongs;
		long[] seq = new long[longs];
		for (int i = 0; i < longs; i++) {
			seq[i] = longSequenceLinearLayout.get(offset + i + 2);
		}
		return new LongSequence(seq, size);
	}

	@Override
	public void add(long id, Sequence sequence) {
		int at = longSequenceLinearLayout.size();
		int size = sequence.length();
		long[] s = ((LongSequence) sequence).array();
		int longs = s.length;
		longSequenceLinearLayout.add(id);
		long sizeAndLongs = (((long) size) << 32) | (long) longs;
		longSequenceLinearLayout.add(sizeAndLongs);
		for (int i = 0; i < longs; i++) {
			longSequenceLinearLayout.add(s[i]);
		}
		nodesWithLongSequences.put(id, at);
	}

	@Override
	public void trim() {
		nodesWithLongSequences.compact();
		longSequenceLinearLayout.trimToSize();
	}

	@Override
	public boolean isEmpty() {
		return nodesWithLongSequences.isEmpty();
	}

	@Override
	public OfLong nodeIds() {
		LongIterator nlsmli = nodesWithLongSequences.keysView().longIterator();
		return fromLongIterator(nlsmli);
	}

	@Override
	public long size() {
		return nodesWithLongSequences.size();
	}

	private boolean seqInNodeSeq(NodeSequence<SimpleNodeHandle> ns, Sequence s) {
		return s.equals(ns.sequence());
	}

	@Override
	public AutoClosedIterator<SimpleNodeHandle> nodeWithSequences(Sequence s) {

		var sequenceMatches = filter(nodeSequences(), ns -> seqInNodeSeq(ns, s));
		return map(sequenceMatches, NodeSequence::node);
	}

	private OfLong fromLongIterator(LongIterator li) {
		return new OfLong() {
			@Override
			public long nextLong() {
				return li.next();
			}

			@Override
			public boolean hasNext() {
				return li.hasNext();
			}
		};
	}

	@Override
	public boolean containsSequence(Sequence s) {
		var iter = new LinearLongSequenceIterator(longSequenceLinearLayout);
		try (var seqs = from(iter)) {
			try (var from = filter(seqs, ns -> seqInNodeSeq(ns, s))) {
				return from.hasNext();
			}
		}
	}

	private static class LinearLongSequenceIterator implements Iterator<NodeSequence<SimpleNodeHandle>> {

		private final LongArrayList longSequenceLinearLayout;

		int offset = 0;

		private LinearLongSequenceIterator(LongArrayList longSequenceLinearLayout) {
			this.longSequenceLinearLayout = longSequenceLinearLayout;
		}

		@Override
		public boolean hasNext() {
			return offset < longSequenceLinearLayout.size();
		}

		@Override
		public NodeSequence<SimpleNodeHandle> next() {
			long nodeid = longSequenceLinearLayout.get(offset++);
			long sizeAndLongs = longSequenceLinearLayout.get(offset++);
			int size = (int) (sizeAndLongs >>> 32);
			int longs = (int) sizeAndLongs;
			long[] seq = new long[longs];
			for (int i = 0; i < longs; i++) {
				seq[i] = longSequenceLinearLayout.get(offset++);
			}
			var node = new SimpleNodeHandle(nodeid);
			var sequence = new LongSequence(seq, size);
			return new NodeSequence<>(node, sequence);
		}
	}

	@Override
	public int maxSequenceLength() {
		int max = 0;
		var iter = new LinearLongSequenceIterator(longSequenceLinearLayout);
		try (var seqs = from(iter)) {
			while (seqs.hasNext()) {
				max = Math.max(0, seqs.next().sequence().length());
			}
		}
		return max;
	}

}
