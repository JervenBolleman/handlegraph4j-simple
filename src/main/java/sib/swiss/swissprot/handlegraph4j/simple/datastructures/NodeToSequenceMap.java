/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sib.swiss.swissprot.handlegraph4j.simple.datastructures;

import io.github.vgteam.handlegraph4j.NodeHandle;
import io.github.vgteam.handlegraph4j.iterators.NodeHandleIterator;
import io.github.vgteam.handlegraph4j.sequences.Sequence;
import io.github.vgteam.handlegraph4j.sequences.SequenceType;
import io.github.vgteam.handlegraph4j.sequences.ShortAmbiguousSequence;
import io.github.vgteam.handlegraph4j.sequences.ShortKnownSequence;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.impl.map.mutable.primitive.LongByteHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.LongLongHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import sib.swiss.swissprot.handlegraph4j.simple.SimpleNodeHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class NodeToSequenceMap {

    private final LongByteHashMap nodeToSingleNP;
    private final LongLongHashMap nodeToSequenceId;
    private final LongObjectHashMap<Sequence> sequenceMap;
    private long maxNodeId;

    public NodeToSequenceMap() {
        this.nodeToSequenceId = new LongLongHashMap();
        this.sequenceMap = new LongObjectHashMap<>();
        this.nodeToSingleNP = new LongByteHashMap();
    }

    public NodeHandleIterator nodes() {
        LongIterator longIterator = nodeToSequenceId.keysView().longIterator();
        return new NodeHandleIterator() {

            @Override
            public boolean hasNext() {
                return longIterator.hasNext();
            }

            @Override
            public NodeHandle next() {
                return new SimpleNodeHandle(longIterator.next());
            }

            @Override
            public void close() throws Exception {
            }
        };
    }

    public Sequence getSequence(SimpleNodeHandle handle) {
        if (nodeToSingleNP.containsKey(handle.id())) {
            return new SingleNucleotideSequence(nodeToSingleNP.get(handle.id()));
        }
        long get = nodeToSequenceId.get(handle.id());
        SequenceType fromLong = SequenceType.fromLong(get);
        switch (fromLong) {
            case SHORT_KNOWN:
                return new ShortKnownSequence(get);
            case SHORT_AMBIGUOUS:
                return new ShortAmbiguousSequence(get);
            case LONG_VIA_ID:
                return sequenceMap.get(get);
        }
        throw new IllegalStateException("A simple node handle was passed in which does not have a sequence");
    }

    public void add(long id, Sequence sequence) {
        maxNodeId = Math.max(maxNodeId, id);
        if (sequence.length() == 1) {
            nodeToSequenceId.put(id, sequence.byteAt(0));
        } else if (sequence.getType() == SequenceType.SHORT_KNOWN) {
            nodeToSequenceId.put(id, ((ShortKnownSequence) sequence).asLong());
        } else if (sequence.getType() == SequenceType.SHORT_AMBIGUOUS) {
            nodeToSequenceId.put(id, ((ShortAmbiguousSequence) sequence).asLong());
        } else {
            long nextId = ((long) sequenceMap.size() + 1) | 2l << 62;
            sequenceMap.put(nextId, sequence);
            nodeToSequenceId.put(id, nextId);
        }
    }

    public long getMaxNodeId() {
        return maxNodeId;
    }
    
    
}
