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

package swiss.sib.swissprot.handlegraph4j.simple;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.BufferedCompressedArrayBackedSteps;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.LongListBackedSteps;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.NodeToSequenceMap;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.Steps;

/**
 *
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 */
public class Path {

    static Path open(RandomAccessFile raf, NodeToSequenceMap nodeToSequenceMap) throws IOException {
        int id = raf.readInt();
        int bytesInNames = raf.readInt();
        char[] charname = new char[bytesInNames];
        for (int i = 0; i < bytesInNames; i++) {
            charname[i] = raf.readChar();
        }
        String name = new String(charname);
        var steps = new BufferedCompressedArrayBackedSteps(raf);
        return new Path(name, id, steps, nodeToSequenceMap);
    }

	static void write(Path path, DataOutputStream raf) throws IOException {
        raf.writeInt(path.id);
        writeName(path, raf);
        BufferedCompressedArrayBackedSteps.write(raf, path.steps);
    }

	private static void writeName(Path path, DataOutputStream raf) throws IOException {
		char[] chName = path.name.toCharArray();
		raf.writeInt(chName.length);
		for (char c : chName) {
			raf.writeChar(c);
		}
	}

    private final String name;
    private final int id;
    private final Steps steps;
    private final NodeToSequenceMap nodeToSequenceMap;

    public Path(String name,
            int id,
            NodeToSequenceMap nodeToSequenceMap) {
        this.name = name;
        this.id = id;
        this.steps = new LongListBackedSteps();
        this.nodeToSequenceMap = nodeToSequenceMap;
    }

	public Path(String name, int id, Steps steps, NodeToSequenceMap nodeToSequenceMap) {
		this.name = name;
		this.id = id;
		this.steps = steps;
		this.nodeToSequenceMap = nodeToSequenceMap;
	}

	public boolean isCircular() {
		return this.steps.firstNode().equals(this.steps.lastNode());
	}

	public AutoClosedIterator<SimpleStepHandle> stepHandles() {
		return new StepHandleIteratorImpl(steps.nodes(), id);
	}

	public long endPositionOfStep(SimpleStepHandle s) {
		try (AutoClosedIterator<SimpleNodeHandle> nodes = steps.nodes()) {
			long endPosition = 0;
			for (int i = 0; nodes.hasNext(); i++) {
				var node = nodes.next();
				int seqLen = nodeToSequenceMap.getSequence(node).length();
				endPosition = endPosition + seqLen;
				if (i == s.rank()) {
					return endPosition;
				} else {
					endPosition++;
				}
			}
			return endPosition;
		}
	}

	long beginPositionOfStep(SimpleStepHandle s) {
		try (AutoClosedIterator<SimpleNodeHandle> nodes = steps.nodes()) {
			long beginPosition = 0;
			for (int i = 0; i < s.rank() && nodes.hasNext(); i++) {
				var node = nodes.next();
				int seqLen = nodeToSequenceMap.getSequence(node).length();
				beginPosition = beginPosition + seqLen + 1;
			}
			return beginPosition;
		}
	}

	long length() {
		return steps.length();
	}

	String name() {
		return name;
	}

	SimplePathHandle pathHandle() {
		return new SimplePathHandle(id);
	}

	SimpleStepHandle stepHandlesByRank(long rank) {
		long nodeIdOfStep = steps.nodeIdOfStep(rank);
		return new SimpleStepHandle(id, nodeIdOfStep, rank);
	}

	AutoClosedIterator<SimpleNodeHandle> nodeHandles() {
		return steps.nodes();
	}

	private static class StepHandleIteratorImpl implements AutoClosedIterator<SimpleStepHandle> {

		private int rank = 0;
		private final AutoClosedIterator<SimpleNodeHandle> nodes;
		private final int pathId;

		public StepHandleIteratorImpl(AutoClosedIterator<SimpleNodeHandle> nodes, int pathId) {
			this.nodes = nodes;
			this.pathId = pathId;
		}

		@Override
		public boolean hasNext() {
			return nodes.hasNext();
		}

		@Override
		public SimpleStepHandle next() {
			int next = rank++;
			long nodeId = nodes.next().id();
			return new SimpleStepHandle(pathId, nodeId, next);
		}

		@Override
		public void close() {
			nodes.close();
		}

	}
}
