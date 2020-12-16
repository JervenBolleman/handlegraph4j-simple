/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures.sequences;

import io.github.vgteam.handlegraph4j.NodeSequence;
import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.filter;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.from;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.map;
import io.github.vgteam.handlegraph4j.sequences.LongSequence;
import io.github.vgteam.handlegraph4j.sequences.Sequence;
import java.util.Iterator;
import java.util.PrimitiveIterator.OfLong;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.LongIntHashMap;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class LongSequenceMap implements NodeSequenceMap {

    private final LongIntHashMap nodesWithLongSequences;
    private final LongArrayList longSequenceLinearLayout;

    public LongSequenceMap(LongIntHashMap nodesWithLongSequences,
            LongArrayList longSequenceLinearLayout) {
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
            seq[i] = longSequenceLinearLayout.get(offset + i + 1);
        }
        return new LongSequence(seq, (int) size);
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
        LongIterator nlsmli = nodesWithLongSequences
                .keysView()
                .longIterator();
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

    private static class LinearLongSequenceIterator
            implements Iterator<NodeSequence<SimpleNodeHandle>> {

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
}
