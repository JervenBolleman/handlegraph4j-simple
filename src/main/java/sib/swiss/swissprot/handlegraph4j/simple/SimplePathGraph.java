/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sib.swiss.swissprot.handlegraph4j.simple;

import sib.swiss.swissprot.handlegraph4j.simple.datastructures.SimpleEdgeList;
import io.github.vgteam.handlegraph4j.PathGraph;
import io.github.vgteam.handlegraph4j.PathHandle;
import io.github.vgteam.handlegraph4j.iterators.EdgeHandleIterator;
import io.github.vgteam.handlegraph4j.iterators.NodeHandleIterator;
import io.github.vgteam.handlegraph4j.iterators.PathHandleIterator;
import io.github.vgteam.handlegraph4j.iterators.StepHandleIterator;
import io.github.vgteam.handlegraph4j.sequences.Sequence;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
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
    public PathHandleIterator<SimplePathHandle> paths() {
        final Iterator<SimplePathHandle> iter = paths.values().iterator();
        return new PathHandleIterator() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public PathHandle next() {
                return iter.next();
            }

            @Override
            public void close() throws Exception {
            }
        };
    }

    @Override
    public StepHandleIterator steps() {
        Iterator<StepHandleIterator> map = paths.values().stream().map(this::stepsOf).iterator();

        return new StepHandleIterator() {
            StepHandleIterator current = null;

            @Override
            public boolean hasNext() {
                while (current == null && map.hasNext()) {
                    current = map.next();
                    if (! current.hasNext())
                        current = null;
                }
                if (current != null && current.hasNext()) {
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public Object next() {
                Object next = current.next();
                if (!current.hasNext())
                    current = null;
                return next;
            }

            @Override
            public void close() throws Exception {
                
            }

        };
    }

    @Override
    public StepHandleIterator stepsOf(SimplePathHandle ph) {
        LongIterator longIterator = pathsToSteps.get(ph).longIterator();
        return new StepHandleIteratorImpl(longIterator, ph.id());
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
    public SimpleEdgeHandle edge(long leftId, long rightId) {
        return new SimpleEdgeHandle(leftId, rightId);
    }

    @Override
    public EdgeHandleIterator<SimpleNodeHandle, SimpleEdgeHandle> followEdges(SimpleNodeHandle left, boolean b) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public EdgeHandleIterator<SimpleNodeHandle, SimpleEdgeHandle> edges() {
        return edges.iterator();
    }

    @Override
    public NodeHandleIterator nodes() {
        return nodeToSequenceMap.nodes();
    }

    @Override
    public SimpleNodeHandle getNodeHandle(SimpleStepHandle s) {
        return new SimpleNodeHandle(s.nodeId());
    }

    @Override
    public Sequence getSequence(SimpleNodeHandle handle) {
        return nodeToSequenceMap.getSequence(handle);
    }

    private static class StepHandleIteratorImpl implements StepHandleIterator<SimpleStepHandle> {

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

        @Override
        public void close() throws Exception {
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

}
