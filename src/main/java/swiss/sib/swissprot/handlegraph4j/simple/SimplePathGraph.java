/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple;

import io.github.vgteam.handlegraph4j.NodeSequence;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.*;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.SimpleEdgeList;
import io.github.vgteam.handlegraph4j.PathGraph;
import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import io.github.vgteam.handlegraph4j.sequences.Sequence;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PrimitiveIterator;
import static java.util.Spliterator.DISTINCT;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterator.SIZED;
import static java.util.Spliterators.spliterator;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.NodeToSequenceMap;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class SimplePathGraph implements PathGraph<SimplePathHandle, SimpleStepHandle, SimpleNodeHandle, SimpleEdgeHandle> {

    private final NodeToSequenceMap nodeToSequenceMap;
    private final Map<String, SimplePathHandle> paths;
    private final Map<SimplePathHandle, long[]> pathsToSteps;
    private final SimpleEdgeList edges;

    public SimplePathGraph(NodeToSequenceMap nodeToSequenceMap, Map<String, SimplePathHandle> paths, Map<SimplePathHandle, long[]> pathsToSteps, SimpleEdgeList edges) {
        this.nodeToSequenceMap = nodeToSequenceMap;
        this.paths = paths;
        this.pathsToSteps = pathsToSteps;
        this.edges = edges;
    }

    @Override
    public AutoClosedIterator<SimplePathHandle> paths() {
        return from(paths.values().iterator());
    }

    @Override
    public AutoClosedIterator<SimpleStepHandle> steps() {
        var p = paths.values().iterator();
        var i = new Iterator<Iterator<SimpleStepHandle>>() {

            @Override
            public boolean hasNext() {
                return p.hasNext();
            }

            @Override
            public Iterator<SimpleStepHandle> next() {
                return stepsOf(p.next());
            }
        };
        var ci = AutoClosedIterator.map(from(i), AutoClosedIterator::from);
        return AutoClosedIterator.flatMap(ci);
    }

    @Override
    public AutoClosedIterator<SimpleStepHandle> stepsOf(SimplePathHandle ph) {
        long[] stepsOfPath = pathsToSteps.get(ph);
        if (stepsOfPath != null) {
            var i = new StepHandleIteratorImpl(stepsOfPath, ph.id());
            return from(i);
        } else {
            return from(Collections.emptyIterator());
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
        return pathsToSteps.get(p).length;
    }

    @Override
    public Sequence sequenceOf(SimpleNodeHandle handle) {
        return nodeToSequenceMap.getSequence(handle);
    }

    private static class StepHandleIteratorImpl implements Iterator<SimpleStepHandle> {

        private final long[] steps;
        private int rank = 0;
        private final int pathId;

        public StepHandleIteratorImpl(long[] steps, int pathId) {
            this.steps = steps;
            this.pathId = pathId;
        }

        @Override
        public boolean hasNext() {
            return rank < steps.length;
        }

        @Override
        public SimpleStepHandle next() {
            int next = rank++;
            long nodeId = steps[next];
            return new SimpleStepHandle(pathId, nodeId, next);
        }
    }

    @Override
    public boolean isCircular(SimplePathHandle path) {
        long[] get = pathsToSteps.get(path);
        long firstNodeId = get[0];
        long lastNodeId = get[get.length - 1];
        return firstNodeId == lastNodeId;
    }

    @Override
    public String nameOfPath(SimplePathHandle p) {
        for (Entry<String, SimplePathHandle> en : paths.entrySet()) {
            if (en.getValue().equals(p)) {
                return en.getKey();
            }
        }
        return null;
    }

    @Override
    public SimplePathHandle pathByName(String name) {
        return paths.get(name);
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
            long[] stepNodeIds = pathsToSteps.get(new SimplePathHandle(s.pathId()));
            long beginPosition = 0;
            for (int i = 0; i < s.rank() && i < stepNodeIds.length; i++) {
                beginPosition = beginPosition + sequenceOf(new SimpleNodeHandle(stepNodeIds[i])).length() + 1;
            }
            return beginPosition;
        }
    }

    @Override
    public long endPositionOfStep(SimpleStepHandle s) {
        if (nodeToSequenceMap.areAllSequencesOneBaseLong()) {
            return s.rank() + 1;
        } else {
            SimplePathHandle path = new SimplePathHandle(s.pathId());
            long[] stepNodeIds = pathsToSteps.get(path);
            long endPosition = 0;
            for (int i = 0; i <= s.rank() && i < stepNodeIds.length; i++) {
                var simpleNodeHandle = new SimpleNodeHandle(stepNodeIds[i]);
                Sequence sequenceOf = sequenceOf(simpleNodeHandle);
                endPosition = endPosition + sequenceOf.length() + 1;
            }
            return endPosition;
        }
    }

    @Override
    public SimpleStepHandle stepByRankAndPath(SimplePathHandle path, long rank) {
        long nodeIdOfStep = pathsToSteps.get(path)[(int) rank];
        return new SimpleStepHandle(path.id(), nodeIdOfStep, rank);
    }

    @Override
    public AutoClosedIterator<SimpleNodeHandle> nodesWithSequence(Sequence s) {
        return nodeToSequenceMap.nodesWithSequence(s);
        
    }

    @Override
    public LongStream positionsOf(SimplePathHandle p) {
        // If all sequences are of length one we know exactly which positions 
        // are possible
        if (nodeToSequenceMap.areAllSequencesOneBaseLong()) {
            return LongStream.range(1, this.stepCountInPath(p) + 1);
        } else {
            var primitiveIter = new PrimitiveIterator.OfLong() {
                private long beginPosition = 0;
                final long[] stepNodeIds = pathsToSteps.get(p);
                boolean begin = true;
                private int at = 0;

                @Override
                public long nextLong() {
                    if (begin) {
                        long toReturn = beginPosition;
                        long next = stepNodeIds[at++];
                        var node = new SimpleNodeHandle(next);
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
                        return at < stepNodeIds.length;
                    }
                }

            };

            var si = spliterator(primitiveIter,
                    pathsToSteps.get(p).length * 2,
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
            final long[] stepNodeIds = pathsToSteps.get(path);
            long beginPosition = 0;
            for (int rank = 0; rank < stepNodeIds.length; rank++) {
                var node = new SimpleNodeHandle(stepNodeIds[rank]);
                beginPosition = beginPosition + sequenceOf(node).length() + 1;
                if (beginPosition == position) {
                    return stepByRankAndPath(path, rank);
                } else if (beginPosition >= position) {
                    return null;
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
            long[] stepNodeIds = pathsToSteps.get(path);
            long endPosition = 0;
            for (int rank = 0; rank < stepNodeIds.length; rank++) {
                var node = new SimpleNodeHandle(stepNodeIds[rank]);
                endPosition = endPosition + sequenceOf(node).length();
                if (endPosition == position) {
                    return stepByRankAndPath(path, rank);
                } else if (endPosition >= position) {
                    return null;
                }
            }
            return null;
        }
    }

    @Override
    public AutoClosedIterator<NodeSequence<SimpleNodeHandle>> nodesWithTheirSequence() {
        return from(nodeToSequenceMap.nodeWithSequenceIterator());
    }
}
