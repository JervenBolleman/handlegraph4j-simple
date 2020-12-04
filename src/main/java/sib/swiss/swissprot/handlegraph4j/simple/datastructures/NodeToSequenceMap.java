/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sib.swiss.swissprot.handlegraph4j.simple.datastructures;

import io.github.vgteam.handlegraph4j.sequences.Sequence;
import io.github.vgteam.handlegraph4j.sequences.SequenceType;
import io.github.vgteam.handlegraph4j.sequences.ShortAmbiguousSequence;
import io.github.vgteam.handlegraph4j.sequences.ShortKnownSequence;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.eclipse.collections.impl.map.mutable.primitive.LongLongHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import org.roaringbitmap.longlong.Roaring64Bitmap;
import sib.swiss.swissprot.handlegraph4j.simple.SimpleNodeHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class NodeToSequenceMap {

    private final Map<Byte, Roaring64Bitmap> singleNps = new HashMap<>();
    private final LongLongHashMap nodeToSequenceId;
    private final LongObjectHashMap<Sequence> sequenceMap;
    private long maxNodeId;

    public NodeToSequenceMap() {
        this.nodeToSequenceId = new LongLongHashMap();
        this.sequenceMap = new LongObjectHashMap<>();
        for (Character ch : Sequence.KNOWN_IUPAC_CODES) {
            singleNps.put((byte) ch.charValue(), new Roaring64Bitmap());
        }
    }

    public Stream<SimpleNodeHandle> nodes() {
        return nodesIds()
                .mapToObj(id -> new SimpleNodeHandle(id));
    }

    public Sequence getSequence(SimpleNodeHandle handle) {
        for (Entry<Byte, Roaring64Bitmap> en : singleNps.entrySet()) {
            if (en.getValue().contains(handle.id())) {
                return new SingleNucleotideSequence(en.getKey());
            }
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
            singleNps.get(sequence.byteAt(0)).addLong(id);
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

    public void trim() {
        for (Iterator<Entry<Byte, Roaring64Bitmap>> it = singleNps.entrySet().iterator(); it.hasNext();) {
            Entry<Byte, Roaring64Bitmap> en = it.next();
            if (en.getValue().isEmpty()) {
                it.remove();
            } else {
                en.getValue().runOptimize();
            }
        }
    }

    public long getMaxNodeId() {
        return maxNodeId;
    }

    public boolean areAllSequencesOneBaseLong() {

        return (!singleNps.isEmpty() && nodeToSequenceId.isEmpty());
    }

    public LongStream nodesIds() {
        return LongStream.concat(
                singleNps.values()
                        .stream()
                        .flatMapToLong(this::nodeIdsFromSingleNps),
                nodeIdsFromNodeToSequenceMap());
    }

    private LongStream nodeIdsFromSingleNps(Roaring64Bitmap nodeIds) throws UnsupportedOperationException {
        if (nodeIds == null) {
            return LongStream.empty();
        } else {
            PrimitiveIterator.OfLong primitiveIter = new PrimitiveIterator.OfLong() {
                Iterator<Long> iterator = nodeIds.iterator();

                @Override
                public long nextLong() {
                    return iterator.next();
                }

                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

            };

            Spliterator.OfLong spliterator = Spliterators.spliterator(primitiveIter, nodeIds.getIntCardinality(), Spliterator.SIZED | Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.NONNULL);
            return StreamSupport.longStream(spliterator, false);
        }
    }

    private LongStream nodeIdsFromNodeToSequenceMap() {
        var longIterator = nodeToSequenceId.keysView().longIterator();
        PrimitiveIterator.OfLong primitiveIter = new PrimitiveIterator.OfLong() {

            @Override
            public long nextLong() {
                return longIterator.next();
            }

            @Override
            public boolean hasNext() {
                return longIterator.hasNext();
            }
        };
        Spliterator.OfLong spliterator = Spliterators.spliterator(primitiveIter, nodeToSequenceId.size(), Spliterator.SIZED | Spliterator.NONNULL);
        return StreamSupport.longStream(spliterator, false);
    }
}
