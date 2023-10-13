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
package swiss.sib.swissprot.handlegraph4j.simple.index;

import static java.util.Spliterator.DISTINCT;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterator.SIZED;
import static java.util.Spliterators.spliterator;

import java.util.HashMap;
import java.util.Map;
import java.util.PrimitiveIterator.OfLong;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

import swiss.sib.swissprot.handlegraph4j.simple.SimplePathGraph;
import swiss.sib.swissprot.handlegraph4j.simple.SimplePathHandle;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleStepHandle;

/**
 *
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 */
public class PositionPathIntArrayIndex implements PositionIndex {

	private final Map<SimplePathHandle, int[]> pathPositions = new HashMap<>();
	private final SimplePathGraph spg;

	public PositionPathIntArrayIndex(SimplePathGraph spg) {
		this.spg = spg;
		byte[] seqLengths = buildSequenceLengthIndex(spg);
		try (var paths = spg.paths()) {
			while (paths.hasNext()) {
				var path = paths.next();
				int stepCountInPath = (int) spg.stepCountInPath(path);
				int[] positions = new int[stepCountInPath + 1];
				pathPositions.put(path, positions);
				int rank = 0;
				try (var steps = spg.stepsOf(path)) {
					int beginPosition = 0;
					positions[rank] = beginPosition;
					while (steps.hasNext()) {
						SimpleStepHandle step = steps.next();
						byte sl = getSequenceLength(spg, step, seqLengths);
						beginPosition += sl + 1;
						positions[++rank] = beginPosition;
					}
				}
			}
		}
	}

	private byte getSequenceLength(SimplePathGraph spg, SimpleStepHandle step, byte[] seqLengths) {
		long nodeId = spg.nodeOfStep(step).id();
		return seqLengths[(int) nodeId];
	}

	private byte[] buildSequenceLengthIndex(SimplePathGraph spg) {
		byte[] seqLengths = new byte[(int) spg.nodeCount() + 1];

		try (var nodes = spg.nodesWithTheirSequence()) {
			while (nodes.hasNext()) {
				var ns = nodes.next();
				seqLengths[(int) ns.node().id()] = (byte) ns.sequence().length();
			}
		}
		return seqLengths;
	}

	@Override
	public int endPositionOfStep(SimpleStepHandle s) {
		long rankOfStep = spg.rankOfStep(s);
		SimplePathHandle pathOfStep = spg.pathOfStep(s);
		int[] get = pathPositions.get(pathOfStep);
		return get[(int) rankOfStep + 1] - 1;
	}

	@Override
	public int beginPositionOfStep(SimpleStepHandle s) {
		long rankOfStep = spg.rankOfStep(s);

		SimplePathHandle pathOfStep = spg.pathOfStep(s);
		return pathPositions.get(pathOfStep)[(int) rankOfStep];
	}

	@Override
	public LongStream positionsOf(SimplePathHandle p) {
		int[] get = pathPositions.get(p);
		OfLong positions = new OfLong() {
			private int cursor = 0;
			boolean begin = true;

			@Override
			public long nextLong() {
				if (begin) {
					return get[cursor];
				} else {
					begin = true;
					return get[++cursor] - 1;
				}
			}

			@Override
			public boolean hasNext() {
				if (!begin) {
					return true;
				} else {
					return cursor < get.length;
				}
			}

		};
		var si = spliterator(positions, get.length * 2, SIZED | ORDERED | DISTINCT | NONNULL);
		return StreamSupport.longStream(si, false);
	}
}
