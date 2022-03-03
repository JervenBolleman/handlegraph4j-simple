/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures.sequences;

import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.filter;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.from;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.map;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.PrimitiveIterator;
import java.util.function.Predicate;

import io.github.vgteam.handlegraph4j.NodeSequence;
import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import io.github.vgteam.handlegraph4j.sequences.Sequence;
import io.github.vgteam.handlegraph4j.sequences.SequenceType;
import io.github.vgteam.handlegraph4j.sequences.ShortAmbiguousSequence;
import io.github.vgteam.handlegraph4j.sequences.ShortKnownSequence;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.LongLongSpinalList;
import swiss.sib.swissprot.handlegraph4j.simple.functions.ToLong;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class MediumSequenceMap implements NodeSequenceMap {

    public static void writeToDisk(MediumSequenceMap nodesWithMediumSequences, DataOutputStream raf) throws IOException {
        nodesWithMediumSequences.nodeSequences.toStream(raf);
    }

    public static MediumSequenceMap open(RandomAccessFile raf) throws IOException {
        ToLong<NodeSequence<SimpleNodeHandle>> gn = MediumSequenceMap::getNodeId;
        ToLong<NodeSequence<SimpleNodeHandle>> gs = MediumSequenceMap::getSequenceAsLong;
        var nodeSequences = new LongLongSpinalList<>(
                MediumSequenceMap::reconstruct,
                gn,
                gs,
                MediumSequenceMap::compareNodeSequence);
        nodeSequences.fromStream(raf);
        return new MediumSequenceMap(nodeSequences);
    }

    private final LongLongSpinalList<NodeSequence<SimpleNodeHandle>> nodeSequences;

    public MediumSequenceMap(LongLongSpinalList<NodeSequence<SimpleNodeHandle>> nodesWithMediumSequences) {
        this.nodeSequences = nodesWithMediumSequences;
    }

    private static int compareNodeSequence(NodeSequence<?> o1, NodeSequence<?> o2) {
        return Long.compare(o1.node().id(), o2.node().id());
    }

    private static long getNodeId(NodeSequence<SimpleNodeHandle> ns) {
        return ns.node().id();
    }

    private static long getSequenceAsLong(NodeSequence<SimpleNodeHandle> ns) {
        return sequenceAsLong(ns.sequence());
    }

    private static long sequenceAsLong(Sequence s) {
        long sl = 0;
        if (s instanceof ShortKnownSequence) {
            return ((ShortKnownSequence) s).asLong();
        } else if (s instanceof ShortAmbiguousSequence) {
            return ((ShortAmbiguousSequence) s).asLong();
        }
        return sl;
    }

    private static NodeSequence<SimpleNodeHandle> reconstruct(long key, long value) {
        var node = new SimpleNodeHandle(key);
        return new NodeSequence<>(node, sequenceFromEncodedLong(value));
    }

    public MediumSequenceMap() {
        ToLong<NodeSequence<SimpleNodeHandle>> gn = MediumSequenceMap::getNodeId;
        ToLong<NodeSequence<SimpleNodeHandle>> gs = MediumSequenceMap::getSequenceAsLong;
        this.nodeSequences = new LongLongSpinalList<>(
                MediumSequenceMap::reconstruct,
                gn,
                gs,
                MediumSequenceMap::compareNodeSequence);
    }

    @Override
    public AutoClosedIterator<NodeSequence<SimpleNodeHandle>> nodeSequences() {
        return from(nodeSequences.iterator());
    }

    @Override
    public Sequence getSequence(long id) {
        var s = nodeSequences.iterateWithKey(id);
        if (s.hasNext()) {
            NodeSequence<SimpleNodeHandle> next = s.next();
            return next.sequence();
        }
        return null;
    }

    private static Sequence sequenceFromEncodedLong(long sequence) {
        SequenceType fromLong = SequenceType.fromLong(sequence);
        switch (fromLong) {
            case SHORT_KNOWN:
                return new ShortKnownSequence(sequence);
            case SHORT_AMBIGUOUS:
                return new ShortAmbiguousSequence(sequence);
            default:
                assert false : "Not valid sequence " + sequence;
                return null;
        }
    }

    @Override
    public PrimitiveIterator.OfLong nodeIds() {
        return nodeSequences.keyIterator();
    }

    public void add(NodeSequence<SimpleNodeHandle> ns) {
        nodeSequences.add(ns);
    }

    @Override
    public void trim() {
        nodeSequences.trimAndSort();
    }

    @Override
    public boolean isEmpty() {
        return nodeSequences.size() == 0;
    }

    @Override
    public long size() {
        return nodeSequences.size();
    }

    @Override
    public AutoClosedIterator<SimpleNodeHandle> nodeWithSequences(Sequence s) {
        long sl = sequenceAsLong(s);
        var from = from(nodeSequences());
        Predicate<NodeSequence<SimpleNodeHandle>> filter = (ns) -> sequenceAsLong(ns.sequence()) == sl;
        return map(filter(from, filter), NodeSequence::node);
    }

    @Override
    public void add(long id, Sequence sequence) {
        SimpleNodeHandle node = new SimpleNodeHandle(id);
        var ns = new NodeSequence<>(node, sequence);
        add(ns);
    }

    @Override
    public boolean containsSequence(Sequence s) {
        long sl = sequenceAsLong(s);
        try ( AutoClosedIterator<NodeSequence<SimpleNodeHandle>> nodeSequences1 = nodeSequences()) {
            var from = from(nodeSequences1);
            Predicate<NodeSequence<SimpleNodeHandle>> filter = (ns) -> sequenceAsLong(ns.sequence()) == sl;
            return filter(from, filter).hasNext();
        }
    }

    @Override
    public int maxSequenceLength() {

        PrimitiveIterator.OfLong valueIterator = nodeSequences.valueIterator();
        int max = 0;
        while (valueIterator.hasNext()) {
            long next = valueIterator.next();
            int length = sequenceFromEncodedLong(next).length();
            max = Math.max(max, length);
        }
        return max;
    }
}
