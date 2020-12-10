/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import io.github.vgteam.handlegraph4j.sequences.AutoClosedIterator;
import io.github.vgteam.handlegraph4j.sequences.LongSequence;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterator.DISTINCT;

import io.github.vgteam.handlegraph4j.sequences.Sequence;
import io.github.vgteam.handlegraph4j.sequences.SequenceType;
import io.github.vgteam.handlegraph4j.sequences.ShortAmbiguousSequence;
import io.github.vgteam.handlegraph4j.sequences.ShortKnownSequence;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PrimitiveIterator.OfLong;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.LongIntHashMap;
import org.roaringbitmap.longlong.Roaring64Bitmap;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class NodeToSequenceMap {

    private final Map<Sequence, Roaring64Bitmap> fewNps = new HashMap<>();
    private final LongLongSpinalList<NodeSequence> nodeToShortSequenceMap;
    private final LongIntHashMap nodeToLongSequencePositionMap;
    private final LongArrayList longSequenceLinearLayout;
    private long maxNodeId;
    private static final int FEW_NUCLEOTIDES_LIMIT = 3;

    private static class NodeSequence {

        private long nodeId;
        private long sequence;

        public NodeSequence(long nodeId, long sequence) {
            this.nodeId = nodeId;
            this.sequence = sequence;
        }

        public long nodeId() {
            return nodeId;
        }

        public long sequence() {
            return sequence;
        }
    }

    private static class NodeSequenceComparator implements
            Comparator<NodeSequence> {

        @Override
        public int compare(NodeSequence o1, NodeSequence o2) {
            return Long.compare(o1.nodeId(), o2.nodeId());
        }

    }

    public NodeToSequenceMap() {
        this.nodeToShortSequenceMap = new LongLongSpinalList<>(NodeSequence::new,
                NodeSequence::nodeId,
                NodeSequence::sequence,
                new NodeSequenceComparator()
        );
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

    public Iterator<SimpleNodeHandle> nodeIterator() {
        OfLong ids = nodeIdsIterator();
        return new Iterator<SimpleNodeHandle>() {
            @Override
            public boolean hasNext() {
                return ids.hasNext();
            }

            @Override
            public SimpleNodeHandle next() {
                long id = ids.nextLong();
                return new SimpleNodeHandle(id);
            }
        };
    }

    public Sequence getSequence(SimpleNodeHandle handle) {
        for (Entry<Sequence, Roaring64Bitmap> en : fewNps.entrySet()) {
            if (en.getValue().contains(handle.id())) {
                return en.getKey();
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
        var s = nodeToShortSequenceMap.streamToLeft(handle.id()).findAny();
        if (s.isPresent()) {
            long sequence = s.get().sequence;
            SequenceType fromLong = SequenceType.fromLong(sequence);
            switch (fromLong) {
                case SHORT_KNOWN:
                    return new ShortKnownSequence(sequence);
                case SHORT_AMBIGUOUS:
                    return new ShortAmbiguousSequence(sequence);
            }
        }
        throw new IllegalStateException("A simple node handle was passed in which does not have a sequence");
    }

    public void add(long id, Sequence sequence) {
        maxNodeId = Math.max(maxNodeId, id);
        if (sequence.length() <= FEW_NUCLEOTIDES_LIMIT) {
            fewNps.get(sequence).addLong(id);
        } else if (sequence.getType() == SequenceType.SHORT_KNOWN) {
            long seq = ((ShortKnownSequence) sequence).asLong();
            var ns = new NodeSequence(id, seq);
            nodeToShortSequenceMap.add(ns);
        } else if (sequence.getType() == SequenceType.SHORT_AMBIGUOUS) {
            long seq = ((ShortAmbiguousSequence) sequence).asLong();
            var ns = new NodeSequence(id, seq);
            nodeToShortSequenceMap.add(ns);
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
        nodeToShortSequenceMap.trimAndSort();
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
        return streamFromOfLong(nodeIdsIterator());
    }

    public OfLong nodeIdsIterator() {
        var nssm = nodeToShortSequenceMap.keyIterator();
        LongIterator nlsmli = nodeToLongSequencePositionMap.keysView().longIterator();
        var nlsm = fromLongIterator(nlsmli);
        List<OfLong> li = new ArrayList<>();

        for (Roaring64Bitmap r : fewNps.values()) {
            li.add(iteratorFromBitMap(r));
        }
        li.add(nssm);
        li.add(nlsm);
        return new AutoClosedIterator.CollectingOfLong(li.iterator());
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

    private LongStream nodeIdsFromBitMap(Roaring64Bitmap nodeIds) throws UnsupportedOperationException {
        if (nodeIds == null) {
            return LongStream.empty();
        } else {
            var iterator = iteratorFromBitMap(nodeIds);
            return streamFromOfLong(iterator);
        }
    }

    private OfLong iteratorFromBitMap(Roaring64Bitmap nodeIds) {
        var longIterator = nodeIds.getLongIterator();
        return new OfLong() {
            @Override
            public long nextLong() {
                return longIterator.next();
            }

            @Override
            public boolean hasNext() {
                return longIterator.hasNext();
            }
        };
    }

    private LongStream streamFromOfLong(OfLong iterator) throws UnsupportedOperationException {
        var si = spliteratorUnknownSize(iterator,
                ORDERED | DISTINCT | NONNULL);
        return StreamSupport.longStream(si, false);
    }

    private LongStream nodeIdsFromNodeToSequenceMap() {
        return nodeToShortSequenceMap.keyStream();
    }

    private LongStream nodeIdsFromSequenceMap() {
        LazyLongIterable keysView = nodeToLongSequencePositionMap.keysView();
        var longIterator = keysView.longIterator();
        return longIteratorToStream(longIterator, keysView.size());
    }

    private LongStream longIteratorToStream(LongIterator longIterator, long size) {
        var primitiveIter = new OfLong() {

            @Override
            public long nextLong() {
                return longIterator.next();
            }

            @Override
            public boolean hasNext() {
                return longIterator.hasNext();
            }
        };
        var si = spliteratorUnknownSize(primitiveIter, NONNULL);
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
