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

import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.*;
import static java.util.Spliterator.DISTINCT;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterator.SIZED;
import static java.util.Spliterators.spliterator;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

import io.github.jervenbolleman.handlegraph4j.NodeSequence;
import io.github.jervenbolleman.handlegraph4j.PathGraph;
import io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator;
import io.github.jervenbolleman.handlegraph4j.sequences.Sequence;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.BufferedNodeToSequenceMap;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.NodeToSequenceMap;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.SimpleEdgeList;

/**
 *
 @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 */
public class SimplePathGraph implements PathGraph<SimplePathHandle, SimpleStepHandle, SimpleNodeHandle, SimpleEdgeHandle> {

    private final NodeToSequenceMap nodeToSequenceMap;
    private final Map<String, Path> pathsByName;
    private final Map<SimplePathHandle, Path> pathsByHandle;
    private final SimpleEdgeList edges;

    public SimplePathGraph(NodeToSequenceMap nodeToSequenceMap, Map<SimplePathHandle, Path> pathsToSteps, SimpleEdgeList edges) {
        this.nodeToSequenceMap = nodeToSequenceMap;
        this.pathsByHandle = pathsToSteps;
        this.pathsByName = new HashMap<>();
        for (Path path : pathsByHandle.values()) {
            pathsByName.put(path.name(), path);
        }
        this.edges = edges;
    }

    @Override
    public AutoClosedIterator<SimplePathHandle> paths() {
        return from(pathsByHandle.keySet().iterator());
    }

    @Override
    public AutoClosedIterator<SimpleStepHandle> steps() {
        return flatMap(map(paths(), ph -> stepsOf(ph)));
    }

    @Override
    public AutoClosedIterator<SimpleStepHandle> stepsOf(SimplePathHandle ph) {
        Path p = pathsByHandle.get(ph);
        if (p != null) {
            return p.stepHandles();
        } else {
            return empty();
        }
    }

    @Override
    public boolean isReverseNodeHandle(SimpleNodeHandle nh) {
        return nh.isReverse();
    }

    @Override
    public SimpleNodeHandle flip(SimpleNodeHandle nh) {
        return nh.flip();
    }

    @Override
    public long asLong(SimpleNodeHandle nh) {
        return nh.id();
    }

    @Override
    public SimpleNodeHandle fromLong(long id) {
        return new SimpleNodeHandle(id);
    }

    @Override
    public SimpleEdgeHandle edge(long leftId, long rightId) {
        return new SimpleEdgeHandle(leftId, rightId);
    }

    @Override
    public AutoClosedIterator<SimpleEdgeHandle> followEdgesToWardsTheRight(SimpleNodeHandle left) {
        return edges.iterateToLeft(left);
    }

    @Override
    public AutoClosedIterator<SimpleEdgeHandle> followEdgesToWardsTheLeft(SimpleNodeHandle right) {
        return edges.iterateToRight(right);
    }

    @Override
    public AutoClosedIterator<SimpleEdgeHandle> edges() {
        return from(edges.iterator());
    }

    @Override
    public AutoClosedIterator<SimpleNodeHandle> nodes() {
        return from(nodeToSequenceMap.nodeIterator());
    }

    @Override
    public long nodeCount() {
        return nodeToSequenceMap.count();
    }

    @Override
    public long stepCountInPath(SimplePathHandle p) {
        return pathsByHandle.get(p).length();
    }

    @Override
    public Sequence sequenceOf(SimpleNodeHandle handle) {
        return nodeToSequenceMap.getSequence(handle);
    }
    
    @Override
    public int sequenceLengthOf(SimpleNodeHandle handle) {
        return nodeToSequenceMap.getSequenceLength(handle);
    }

    public void writeTo(DataOutputStream raf) throws IOException {
        nodeToSequenceMap.writeToDisk(raf);
        edges.writeToDisk(raf);
        raf.writeInt(pathsByHandle.size());
        for (Path p : pathsByHandle.values()) {
            Path.write(p, raf);
        }
    }

    public static SimplePathGraph open(RandomAccessFile raf) throws IOException {
        var bufferedNodeToSequenceMap = new BufferedNodeToSequenceMap(raf);
        var edges = new SimpleEdgeList();
        edges.open(raf);
        var pathsToSteps = new HashMap<SimplePathHandle, Path>();
        int numberOfPaths = raf.readInt();
        for (int i = 0; i < numberOfPaths; i++) {
            Path p = Path.open(raf, bufferedNodeToSequenceMap);
            pathsToSteps.put(p.pathHandle(), p);
        }

        return new SimplePathGraph(bufferedNodeToSequenceMap, pathsToSteps, edges);
    }

    @Override
    public boolean isCircular(SimplePathHandle path) {
        return pathsByHandle.get(path).isCircular();
    }

    @Override
    public String nameOfPath(SimplePathHandle p) {
        if (pathsByHandle.containsKey(p)) {
            return pathsByHandle.get(p).name();
        }
        return null;
    }

    @Override
    public SimplePathHandle pathByName(String name) {
        if (pathsByName.containsKey(name)) {
            return pathsByName.get(name).pathHandle();
        } else {
            return null;
        }
    }

    @Override
    public SimplePathHandle pathOfStep(SimpleStepHandle s) {
        return new SimplePathHandle(s.pathId());
    }

    @Override
    public SimpleNodeHandle nodeOfStep(SimpleStepHandle s) {
        return new SimpleNodeHandle(s.nodeId());
    }

    @Override
    public long rankOfStep(SimpleStepHandle s) {
        return s.rank();
    }

    @Override
    public long beginPositionOfStep(SimpleStepHandle s) {
        if (nodeToSequenceMap.areAllSequencesOneBaseLong()) {
            return s.rank();
        } else {
            Path p = pathsByHandle.get(new SimplePathHandle(s.pathId()));
            return p.beginPositionOfStep(s);

        }
    }

    @Override
    public long endPositionOfStep(SimpleStepHandle s) {
        if (nodeToSequenceMap.areAllSequencesOneBaseLong()) {
            return s.rank() + 1;
        } else {
            Path p = pathsByHandle.get(new SimplePathHandle(s.pathId()));
            return p.endPositionOfStep(s);
        }
    }

    @Override
    public SimpleStepHandle stepByRankAndPath(SimplePathHandle path, long rank) {
        Path p = pathsByHandle.get(path);
        return p.stepHandlesByRank(rank);

    }

    @Override
    public AutoClosedIterator<SimpleNodeHandle> nodesWithSequence(Sequence s) {
        return nodeToSequenceMap.nodesWithSequence(s);

    }

    @Override
    public LongStream positionsOf(SimplePathHandle p) {
        // If all sequences are of length one we know exactly which positions 
        // are possible
        var path = pathsByHandle.get(p);
        if (nodeToSequenceMap.areAllSequencesOneBaseLong()) {
            return LongStream.range(1, this.stepCountInPath(p) + 1);
        } else {
            var primitiveIter = new PrimitiveIterator.OfLong() {
                private long beginPosition = 0;
                private final AutoClosedIterator<SimpleNodeHandle> nodes = path.nodeHandles();
                boolean begin = true;

                @Override
                public long nextLong() {
                    if (begin) {
                        long toReturn = beginPosition;
                        var node = nodes.next();
                        int length = sequenceOf(node).length();
                        beginPosition += length;
                        begin = false;
                        return toReturn;
                    } else {
                        begin = true;
                        return beginPosition++;
                    }
                }

                @Override
                public boolean hasNext() {
                    if (!begin) {
                        return true;
                    } else {
                        return nodes.hasNext();
                    }
                }

            };

            var si = spliterator(primitiveIter,
                    path.length() * 2,
                    SIZED | ORDERED | DISTINCT | NONNULL);
            return StreamSupport.longStream(si, false);
        }
    }

    @Override
    public SimpleStepHandle stepOfPathByBeginPosition(SimplePathHandle path,
            long position) {
        if (nodeToSequenceMap.areAllSequencesOneBaseLong()) {
            return stepByRankAndPath(path, position);
        } else {
            Path p = pathsByHandle.get(path);
            try (var stepHandles = p.stepHandles()) {
                long beginPosition = 0;
                while (stepHandles.hasNext()) {
                    SimpleStepHandle step = stepHandles.next();
                    var nodeId = step.nodeId();
                    var node = new SimpleNodeHandle(nodeId);
                    int seqLen = sequenceOf(node).length();
                    beginPosition = beginPosition + seqLen + 1;
                    if (beginPosition == position) {
                        return step;
                    } else if (beginPosition >= position) {
                        return null;
                    }
                }
            }
            return null;
        }
    }

    @Override
    public SimpleStepHandle stepOfPathByEndPosition(SimplePathHandle path, long position) {
        if (nodeToSequenceMap.areAllSequencesOneBaseLong()) {
            return stepByRankAndPath(path, position + 1);
        } else {
            Path p = pathsByHandle.get(path);
            try (var stepHandles = p.stepHandles()) {
                long endPosition = 0;
                while (stepHandles.hasNext()) {
                    var step = stepHandles.next();
                    var node = new SimpleNodeHandle(step.nodeId());
                    int length = sequenceOf(node).length();
                    endPosition = endPosition + length;
                    if (endPosition == position) {
                        return step;
                    } else if (endPosition >= position) {
                        return null;
                    }
                }
            }
            return null;
        }
    }

    @Override
    public AutoClosedIterator<NodeSequence<SimpleNodeHandle>> nodesWithTheirSequence() {
        return from(nodeToSequenceMap.nodeWithSequenceIterator());
    }

    @Override
    public int pathCount() {
        return pathsByName.size();
    }

    @Override
    public long stepCount() {
        return pathsByHandle.values()
                .stream()
                .mapToLong(Path::length)
                .sum();
    }

}
