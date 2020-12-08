/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import io.github.vgteam.handlegraph4j.sequences.LongSequence;
import static java.util.Spliterators.spliterator;
import static java.util.Spliterator.SIZED;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterator.DISTINCT;

import io.github.vgteam.handlegraph4j.sequences.Sequence;
import io.github.vgteam.handlegraph4j.sequences.SequenceType;
import io.github.vgteam.handlegraph4j.sequences.ShortAmbiguousSequence;
import io.github.vgteam.handlegraph4j.sequences.ShortKnownSequence;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PrimitiveIterator;
import java.util.function.Function;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.LongIntHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.LongLongHashMap;
import org.roaringbitmap.longlong.Roaring64Bitmap;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class NodeToSequenceMap {

    private final Map<Sequence, Roaring64Bitmap> fewNps = new HashMap<>();
    private final LongLongHashMap nodeToShortSequenceMap;
    private final LongIntHashMap nodeToLongSequencePositionMap;
    private final LongArrayList longSequenceLinearLayout;
    private long maxNodeId;
    private static final int FEW_NUCLEOTIDES_LIMIT = 3;

    public NodeToSequenceMap() {
        this.nodeToShortSequenceMap = new LongLongHashMap();
        this.nodeToLongSequencePositionMap = new LongIntHashMap();
        this.longSequenceLinearLayout = new LongArrayList();
        initializeVeryShortSequenceMaps();
        for (int i = 4; i < ShortAmbiguousSequence.MAX_LENGTH; i++) {
            byte[] nn = new byte[i];
            Arrays.fill(nn, (byte) 'n');
            fewNps.put(new ShortAmbiguousSequence(nn), new Roaring64Bitmap());
        }
    }

    private void initializeVeryShortSequenceMaps() {
        for (Character ch1 : Sequence.KNOWN_IUPAC_CODES) {
            byte b1 = (byte) ch1.charValue();
            fewNps.put(new SingleNucleotideSequence(b1), new Roaring64Bitmap());
            for (Character ch2 : Sequence.KNOWN_IUPAC_CODES) {
                Roaring64Bitmap rbm = new Roaring64Bitmap();
                byte b2 = (byte) ch2.charValue();
                byte[] ba = new byte[]{b1, b2};
                Sequence seq = SequenceType.fromByteArray(ba);
                fewNps.put(seq, rbm);
                for (Character ch3 : Sequence.KNOWN_IUPAC_CODES) {
                    byte b3 = (byte) ch3.charValue();
                    byte[] ba3 = new byte[]{b1, b2, b3};
                    Sequence seq3 = SequenceType.fromByteArray(ba3);
                    fewNps.put(seq3, new Roaring64Bitmap());
                }
            }
        }
    }

    public Stream<SimpleNodeHandle> nodes() {
        return nodesIds()
                .mapToObj(id -> new SimpleNodeHandle(id));
    }

    public Sequence getSequence(SimpleNodeHandle handle) {
        for (Entry<Sequence, Roaring64Bitmap> en : fewNps.entrySet()) {
            if (en.getValue().contains(handle.id())) {
                return en.getKey();
            }
        }
        if (nodeToShortSequenceMap.containsKey(handle.id())) {
            long sequence = nodeToShortSequenceMap.get(handle.id());

            SequenceType fromLong = SequenceType.fromLong(sequence);
            switch (fromLong) {
                case SHORT_KNOWN:
                    return new ShortKnownSequence(sequence);
                case SHORT_AMBIGUOUS:
                    return new ShortAmbiguousSequence(sequence);

            }
        }
        if (nodeToLongSequencePositionMap.containsKey(handle.id())) {
            int offset = nodeToLongSequencePositionMap.get(handle.id());
            long sizeAndLongs = longSequenceLinearLayout.get(offset);
            int size = (int) (sizeAndLongs >>> 32);
            int longs = (int) sizeAndLongs;
            long[] seq = new long[longs];
            for (int i = 0; i < longs; i++) {
                seq[i] = longSequenceLinearLayout.get(offset + i);
            }
            return new LongSequence(seq, (int) size);
        }
        throw new IllegalStateException("A simple node handle was passed in which does not have a sequence");
    }

    public void add(long id, Sequence sequence) {
        maxNodeId = Math.max(maxNodeId, id);
        if (sequence.length() <= FEW_NUCLEOTIDES_LIMIT) {
            fewNps.get(sequence).addLong(id);
        } else if (sequence.getType()
                == SequenceType.SHORT_KNOWN) {
            nodeToShortSequenceMap.put(id, ((ShortKnownSequence) sequence).asLong());
        } else if (sequence.getType()
                == SequenceType.SHORT_AMBIGUOUS) {
            nodeToShortSequenceMap.put(id, ((ShortAmbiguousSequence) sequence).asLong());
        } else {
            int at = longSequenceLinearLayout.size();
            int size = sequence.length();
            long[] s = ((LongSequence) sequence).array();
            int longs = s.length;
            long sizeAndLongs = (((long) size) << 32) | (long) longs;
            longSequenceLinearLayout.add(sizeAndLongs);
            for (int i = 0; i < longs; i++) {
                longSequenceLinearLayout.add(s[i]);
            }
            nodeToLongSequencePositionMap.put(id, at);
        }
    }

    public void trim() {
        trimFewNps();
        nodeToLongSequencePositionMap.compact();
        nodeToShortSequenceMap.compact();
        longSequenceLinearLayout.trimToSize();
    }

    private void trimFewNps() {
        Iterator<Entry<Sequence, Roaring64Bitmap>> it
                = fewNps.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Sequence, Roaring64Bitmap> en = it.next();
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

        if (nodeToShortSequenceMap.isEmpty() && nodeToLongSequencePositionMap.isEmpty()) {
            return 1 == fewNps.keySet().stream()
                    .mapToInt(Sequence::length)
                    .max()
                    .orElse(0);
        }
        return false;
    }

    public LongStream nodesIds() {
        return Stream.of(
                fewNps.values()
                        .stream()
                        .flatMapToLong(this::nodeIdsFromBitMap),
                nodeIdsFromNodeToSequenceMap(),
                nodeIdsFromSequenceMap())
                .flatMapToLong(Function.identity());
    }

    private LongStream nodeIdsFromBitMap(Roaring64Bitmap nodeIds) throws UnsupportedOperationException {
        if (nodeIds == null) {
            return LongStream.empty();
        } else {
            Iterator<Long> iterator = nodeIds.iterator();
            return streamFromBitMap(iterator, nodeIds);
        }
    }

    private LongStream streamFromBitMap(Iterator<Long> iterator, Roaring64Bitmap nodeIds) throws UnsupportedOperationException {
        var primitiveIter = new PrimitiveIterator.OfLong() {

            @Override
            public long nextLong() {
                return iterator.next();
            }

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

        };

        var si = spliterator(primitiveIter,
                nodeIds.getIntCardinality(),
                SIZED | ORDERED | DISTINCT | NONNULL);
        return StreamSupport.longStream(si, false);
    }

    private LongStream nodeIdsFromNodeToSequenceMap() {
        var longIterator = nodeToShortSequenceMap.keysView().longIterator();
        return longIteratorToStream(longIterator);
    }

    private LongStream nodeIdsFromSequenceMap() {
        var longIterator = nodeToLongSequencePositionMap.keysView().longIterator();
        return longIteratorToStream(longIterator);
    }

    private LongStream longIteratorToStream(LongIterator longIterator) {
        var primitiveIter = new PrimitiveIterator.OfLong() {

            @Override
            public long nextLong() {
                return longIterator.next();
            }

            @Override
            public boolean hasNext() {
                return longIterator.hasNext();
            }
        };
        var si = spliterator(primitiveIter,
                nodeToShortSequenceMap.size(),
                SIZED | NONNULL);
        return StreamSupport.longStream(si, false);
    }

    public long count() {
        long sumSNps = fewNps.values()
                .stream()
                .mapToLong(Roaring64Bitmap::getLongCardinality)
                .sum();
        return sumSNps + nodeToShortSequenceMap.size() + nodeToLongSequencePositionMap.size();
    }
}
