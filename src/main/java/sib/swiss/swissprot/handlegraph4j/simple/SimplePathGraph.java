/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sib.swiss.swissprot.handlegraph4j.simple;

import sib.swiss.swissprot.handlegraph4j.simple.datastructures.SimpleEdgeList;
import io.github.vgteam.handlegraph4j.PathGraph;
import io.github.vgteam.handlegraph4j.sequences.Sequence;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.list.primitive.LongList;
import sib.swiss.swissprot.handlegraph4j.simple.datastructures.NodeToSequenceMap;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class SimplePathGraph implements PathGraph<SimplePathHandle, SimpleStepHandle, SimpleNodeHandle, SimpleEdgeHandle> {

    private final NodeToSequenceMap nodeToSequenceMap;
    private final Map<String, SimplePathHandle> paths;
    private final Map<SimplePathHandle, LongList> pathsToSteps;
    private final SimpleEdgeList edges;

    public SimplePathGraph(NodeToSequenceMap nodeToSequenceMap, Map<String, SimplePathHandle> paths, Map<SimplePathHandle, LongList> pathsToSteps, SimpleEdgeList edges) {
        this.nodeToSequenceMap = nodeToSequenceMap;
        this.paths = paths;
        this.pathsToSteps = pathsToSteps;
        this.edges = edges;
    }

    @Override
    public Stream<SimplePathHandle> paths() {
        return paths.values().stream();
    }

    @Override
    public Stream<SimpleStepHandle> steps() {
        return paths.values().stream().flatMap(this::stepsOf);
    }

    @Override
    public Stream<SimpleStepHandle> stepsOf(SimplePathHandle ph) {
        LongList stepsOfPath = pathsToSteps.get(ph);
        LongIterator longIterator = stepsOfPath.longIterator();
        StepHandleIteratorImpl stepHandleIteratorImpl = new StepHandleIteratorImpl(longIterator, ph.id());
        Spliterator<SimpleStepHandle> spliterator = Spliterators.spliterator(stepHandleIteratorImpl, pathsToSteps.size(), Spliterator.SIZED);
        return StreamSupport.stream(spliterator, false);
    }

    @Override
    public boolean isReverseNodeHandle(SimpleNodeHandle nh) {
        return asLong(nh) < 0;
    }

    @Override
    public SimpleNodeHandle flip(SimpleNodeHandle nh) {
        return new SimpleNodeHandle(-asLong(nh));
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
    public Stream<SimpleEdgeHandle> followEdgesToWardsTheRight(SimpleNodeHandle left) {
        return edges.streamToLeft(left);
    }

    @Override
    public Stream<SimpleEdgeHandle> followEdgesToWardsTheLeft(SimpleNodeHandle right) {
        return edges.streamToRight(right);
    }

    @Override
    public Stream<SimpleEdgeHandle> edges() {
        return edges.stream();
    }

    @Override
    public Stream<SimpleNodeHandle> nodes() {
        return nodeToSequenceMap.nodes();
    }

    @Override
    public SimpleNodeHandle getNodeHandle(SimpleStepHandle s) {
        return new SimpleNodeHandle(s.nodeId());
    }

    @Override
    public Sequence sequenceOf(SimpleNodeHandle handle) {
        return nodeToSequenceMap.getSequence(handle);
    }

    private static class StepHandleIteratorImpl implements Iterator<SimpleStepHandle> {

        private final LongIterator steps;
        private long rank = 0;
        private final int pathId;

        public StepHandleIteratorImpl(LongIterator steps, int pathId) {
            this.steps = steps;
            this.pathId = pathId;
        }

        @Override
        public boolean hasNext() {
            return steps.hasNext();
        }

        @Override
        public SimpleStepHandle next() {
            long next = steps.next();
            return new SimpleStepHandle(pathId, next, rank++);
        }
    }

    @Override
    public boolean isCircular(SimplePathHandle path) {
        LongList get = pathsToSteps.get(path);
        long firstNodeId = get.getFirst();
        long lastNodeId = get.getLast();
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
            LongIterator stepNodeIds = pathsToSteps.get(new SimplePathHandle(s.pathId())).longIterator();
            long beginPosition = 0;
            for (int i = 0; i < s.rank() && stepNodeIds.hasNext(); i++) {
                beginPosition = beginPosition + sequenceOf(new SimpleNodeHandle(stepNodeIds.next())).length();
            }
            return beginPosition;
        }
    }

    @Override
    public long endPositionOfStep(SimpleStepHandle s) {
        if (nodeToSequenceMap.areAllSequencesOneBaseLong()) {
            return s.rank() + 1;
        } else {
            LongIterator stepNodeIds = pathsToSteps.get(new SimplePathHandle(s.pathId())).longIterator();
            long endPosition = 0;
            for (int i = 0; i < s.rank() && stepNodeIds.hasNext(); i++) {
                endPosition = endPosition + sequenceOf(new SimpleNodeHandle(stepNodeIds.next())).length();
            }
            return endPosition;
        }
    }

    @Override
    public SimpleStepHandle stepByRankAndPath(SimplePathHandle path, long rank) {
        long nodeIdOfStep = pathsToSteps.get(path).get((int) rank);
        return new SimpleStepHandle(path.id(), nodeIdOfStep, rank);
    }

    @Override
    public Stream<SimpleNodeHandle> nodesWithSequence(Sequence s) {
        return nodeToSequenceMap.nodesIds()
                .mapToObj(this::fromLong)
                .filter(n -> sequenceOf(n).equals(s));
    }

}
