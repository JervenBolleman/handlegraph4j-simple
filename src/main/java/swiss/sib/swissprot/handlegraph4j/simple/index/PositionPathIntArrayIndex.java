/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.index;

import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import java.util.HashMap;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.PrimitiveIterator.OfLong;
import static java.util.Spliterator.DISTINCT;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterator.SIZED;
import static java.util.Spliterators.spliterator;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;
import swiss.sib.swissprot.handlegraph4j.simple.SimplePathGraph;
import swiss.sib.swissprot.handlegraph4j.simple.SimplePathHandle;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleStepHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
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
                int stepCountInPath  = (int) spg.stepCountInPath(path);
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
        var si = spliterator(positions,
                get.length * 2,
                SIZED | ORDERED | DISTINCT | NONNULL);
        return StreamSupport.longStream(si, false);
    }
}
