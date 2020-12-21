/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple;

import io.github.vgteam.handlegraph4j.NodeSequence;
import io.github.vgteam.handlegraph4j.PathGraph;
import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import io.github.vgteam.handlegraph4j.sequences.Sequence;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.function.BiFunction;
import java.util.stream.LongStream;
import swiss.sib.swissprot.handlegraph4j.simple.index.PositionIndex;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class SimplePositionIndexedPathGraph implements
        PathGraph<SimplePathHandle, SimpleStepHandle, SimpleNodeHandle, SimpleEdgeHandle> {

    private final SimplePathGraph spg;
    private final PositionIndex pi;

    public SimplePositionIndexedPathGraph(SimplePathGraph spg,
            PositionIndex pi) {
        this.spg = spg;
        this.pi = pi;
    }

    @Override
    public AutoClosedIterator<SimplePathHandle> paths() {
        return spg.paths();
    }

    @Override
    public AutoClosedIterator<SimpleStepHandle> steps() {
        return spg.steps();
    }

    @Override
    public AutoClosedIterator<SimpleStepHandle> stepsOf(SimplePathHandle ph) {
        return spg.stepsOf(ph);
    }

    @Override
    public boolean isReverseNodeHandle(SimpleNodeHandle nh) {
        return spg.isReverseNodeHandle(nh);
    }

    @Override
    public SimpleNodeHandle flip(SimpleNodeHandle nh) {
        return spg.flip(nh);
    }

    @Override
    public long asLong(SimpleNodeHandle nh) {
        return spg.asLong(nh);
    }

    @Override
    public SimpleNodeHandle fromLong(long id) {
        return spg.fromLong(id);
    }

    @Override
    public SimpleEdgeHandle edge(long leftId, long rightId) {
        return spg.edge(leftId, rightId);
    }

    @Override
    public AutoClosedIterator<SimpleEdgeHandle> followEdgesToWardsTheRight(SimpleNodeHandle left) {
        return spg.followEdgesToWardsTheRight(left);
    }

    @Override
    public AutoClosedIterator<SimpleEdgeHandle> followEdgesToWardsTheLeft(SimpleNodeHandle right) {
        return spg.followEdgesToWardsTheLeft(right);
    }

    @Override
    public AutoClosedIterator<SimpleEdgeHandle> edges() {
        return spg.edges();
    }

    @Override
    public AutoClosedIterator<SimpleNodeHandle> nodes() {
        return spg.nodes();
    }

    @Override
    public long nodeCount() {
        return spg.nodeCount();
    }

    @Override
    public long stepCountInPath(SimplePathHandle p) {
        return spg.stepCountInPath(p);
    }

    @Override
    public Sequence sequenceOf(SimpleNodeHandle handle) {
        return spg.sequenceOf(handle);
    }

    public void writeTo(DataOutputStream raf) throws IOException {
        spg.writeTo(raf);
    }

    public static SimplePositionIndexedPathGraph open(RandomAccessFile raf,
            BiFunction<RandomAccessFile, SimplePathGraph, PositionIndex> reader)
            throws IOException {
        SimplePathGraph open = SimplePathGraph.open(raf);
        PositionIndex pi = reader.apply(raf, open);
        return new SimplePositionIndexedPathGraph(open, pi);
    }

    @Override
    public boolean isCircular(SimplePathHandle path) {
        return spg.isCircular(path);
    }

    @Override
    public String nameOfPath(SimplePathHandle p) {
        return spg.nameOfPath(p);
    }

    @Override
    public SimplePathHandle pathByName(String name) {
        return spg.pathByName(name);
    }

    @Override
    public SimplePathHandle pathOfStep(SimpleStepHandle s) {
        return spg.pathOfStep(s);
    }

    @Override
    public SimpleNodeHandle nodeOfStep(SimpleStepHandle s) {
        return spg.nodeOfStep(s);
    }

    @Override
    public long rankOfStep(SimpleStepHandle s) {
        return spg.rankOfStep(s);
    }

    @Override
    public long beginPositionOfStep(SimpleStepHandle s) {
        return pi.beginPositionOfStep(s);
    }

    @Override
    public long endPositionOfStep(SimpleStepHandle s) {
        return pi.endPositionOfStep(s);
    }

    @Override
    public SimpleStepHandle stepByRankAndPath(SimplePathHandle path, long rank) {
        return spg.stepByRankAndPath(path, rank);
    }

    @Override
    public AutoClosedIterator<SimpleNodeHandle> nodesWithSequence(Sequence s) {
        return spg.nodesWithSequence(s);
    }

    @Override
    public LongStream positionsOf(SimplePathHandle p) {
        return spg.positionsOf(p);
    }

    @Override
    public SimpleStepHandle stepOfPathByBeginPosition(SimplePathHandle path, long position) {
        return spg.stepOfPathByBeginPosition(path, position);
    }

    @Override
    public SimpleStepHandle stepOfPathByEndPosition(SimplePathHandle path, long position) {
        return spg.stepOfPathByEndPosition(path, position);
    }

    @Override
    public AutoClosedIterator<NodeSequence<SimpleNodeHandle>> nodesWithTheirSequence() {
        return spg.nodesWithTheirSequence();
    }

}
