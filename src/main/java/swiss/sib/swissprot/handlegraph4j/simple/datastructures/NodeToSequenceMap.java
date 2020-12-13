/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import io.github.vgteam.handlegraph4j.NodeSequence;
import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.flatMap;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.map;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.empty;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.filter;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.from;
import static swiss.sib.swissprot.handlegraph4j.simple.datastructures.LongLongSpinalList.ToLong;
import io.github.vgteam.handlegraph4j.iterators.CollectingOfLong;
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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.tuple.primitive.LongIntPair;
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
    private final LongLongSpinalList<NodeSequence<SimpleNodeHandle>> nodeToShortSequenceMap;
    private final LongIntHashMap nodeToLongSequencePositionMap;
    private final LongArrayList longSequenceLinearLayout;
    private long maxNodeId;
    private static final int FEW_NUCLEOTIDES_LIMIT = 3;

    private static class NodeSequenceComparator implements
            Comparator<NodeSequence<SimpleNodeHandle>> {

        @Override
        public int compare(NodeSequence o1, NodeSequence o2) {
            return Long.compare(o1.node().id(), o2.node().id());
        }
    }

    private static final ToLong<NodeSequence<SimpleNodeHandle>> GET_KEY
            = ns -> ns.node().id();
    private static final ToLong<NodeSequence<SimpleNodeHandle>> GET_VALUE
            = ns -> sequenceAsLong(ns.sequence());

    public NodeToSequenceMap() {
        this.nodeToShortSequenceMap = new LongLongSpinalList<>((key, value) -> {
            var node = new SimpleNodeHandle(key);
            return new NodeSequence<>(node, sequenceFromEncodedLong(value));
        },
                GET_KEY,
                GET_VALUE,
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

    public Iterator<NodeSequence<SimpleNodeHandle>> nodeWithSequenceIterator() {
        var smalls = nodesWithSmallSequences();

        var longs = nodesWithLongSequences();
        var iters = nodesWithMediumKnownSequences(smalls, longs);
        return AutoClosedIterator.flatMap(iters);
    }

    private AutoClosedIterator<AutoClosedIterator<NodeSequence<SimpleNodeHandle>>> nodesWithMediumKnownSequences(AutoClosedIterator<NodeSequence<SimpleNodeHandle>> smalls, AutoClosedIterator<NodeSequence<SimpleNodeHandle>> longs) {
        var mediums = from(nodeToShortSequenceMap.iterator());
        var iters = AutoClosedIterator.of(smalls, mediums, longs);
        return iters;
    }

    private class LongIntToSequence implements Function<LongIntPair, NodeSequence<SimpleNodeHandle>> {

        @Override
        public NodeSequence<SimpleNodeHandle> apply(LongIntPair p) {
            var node = new SimpleNodeHandle(p.getOne());
            var seq = getLongSequence(p.getTwo());
            return new NodeSequence<>(node, seq);
        }
    }

    private class LongToSequence implements Function<Long, NodeSequence<SimpleNodeHandle>> {

        private final Sequence seq;

        public LongToSequence(Sequence seq) {
            this.seq = seq;
        }

        @Override
        public NodeSequence<SimpleNodeHandle> apply(Long id) {
            var node = new SimpleNodeHandle(id);
            return new NodeSequence<>(node, seq);
        }
    }

    private class SequenceAndRoaringBitMap implements
            Function<Entry<Sequence, Roaring64Bitmap>, AutoClosedIterator<NodeSequence<SimpleNodeHandle>>> {

        @Override
        public AutoClosedIterator<NodeSequence<SimpleNodeHandle>> apply(Entry<Sequence, Roaring64Bitmap> en) {
            Sequence s = en.getKey();
            Roaring64Bitmap b = en.getValue();
            return map(iteratorFromBitMap(b), new LongToSequence(s));
        }

    }

    private AutoClosedIterator<NodeSequence<SimpleNodeHandle>> nodesWithLongSequences() {
        var longToOffsetView = from(nodeToLongSequencePositionMap.keyValuesView()
                .iterator());

        var longs = map(longToOffsetView, new LongIntToSequence());
        return longs;
    }

    private AutoClosedIterator<NodeSequence<SimpleNodeHandle>> nodesWithSmallSequences() {
        var smalls = flatMap(map(fewNps.entrySet().iterator(), new SequenceAndRoaringBitMap()));
        return smalls;
    }

    public AutoClosedIterator<SimpleNodeHandle> nodesWithSequence(Sequence s) {
        if (s == null) {
            return empty();
        }
        if (fewNps.containsKey(s)) {
            OfLong asLong = iteratorFromBitMap(fewNps.get(s));
            return map(asLong, SimpleNodeHandle::new);
        }
        if (s.getType() == SequenceType.LONG_VIA_ID) {
            var keyValuesView = nodeToLongSequencePositionMap.keyValuesView();
            AutoClosedIterator<LongIntPair> from = from(keyValuesView.iterator());
            var idSeqOfset = map(from, p -> {
                int so = p.getTwo();
                long id = p.getOne();

                return new Object[]{id, getLongSequence(so)};
            });
            var filter = filter(idSeqOfset, a -> s.equals(a[1]));
            return map(filter, a -> new SimpleNodeHandle((long) a[0]));
        } else {
            long sl = sequenceAsLong(s);
            var from = from(nodeToShortSequenceMap.iterator());
            Predicate<NodeSequence<SimpleNodeHandle>> filter = (ns) -> sequenceAsLong(ns.sequence()) == sl;
            Function<NodeSequence<SimpleNodeHandle>, SimpleNodeHandle> mapper = ns -> ns.node();
            return map(filter(from, filter), mapper);
        }
    }

    private static long sequenceAsLong(Sequence s) {
        long sl = 0;
        if (s instanceof ShortKnownSequence) {
            sl = ((ShortKnownSequence) s).asLong();
        } else if (s instanceof ShortAmbiguousSequence) {
            sl = ((ShortAmbiguousSequence) s).asLong();
        }
        return sl;
    }

    public Sequence getSequence(SimpleNodeHandle handle) {
        for (Entry<Sequence, Roaring64Bitmap> en : fewNps.entrySet()) {
            if (en.getValue().contains(handle.id())) {
                return en.getKey();
            }
        }

        if (nodeToLongSequencePositionMap.containsKey(handle.id())) {
            int offset = nodeToLongSequencePositionMap.get(handle.id());
            return getLongSequence(offset);
        }
        var s = nodeToShortSequenceMap.iterateToLeft(handle.id());
        if (s.hasNext()) {
            return s.next().sequence();
        }
        throw new IllegalStateException("A simple node handle was passed in which does not have a sequence");
    }

    private static Sequence sequenceFromEncodedLong(long sequence) {
        SequenceType fromLong = SequenceType.fromLong(sequence);
        switch (fromLong) {
            case SHORT_KNOWN:
                return new ShortKnownSequence(sequence);
            case SHORT_AMBIGUOUS:
                return new ShortAmbiguousSequence(sequence);
            default:
                return null;
        }
    }

    private Sequence getLongSequence(int offset) {
        long sizeAndLongs = longSequenceLinearLayout.get(offset);
        int size = (int) (sizeAndLongs >>> 32);
        int longs = (int) sizeAndLongs;
        long[] seq = new long[longs];
        for (int i = 0; i < longs; i++) {
            seq[i] = longSequenceLinearLayout.get(offset + i);
        }
        return new LongSequence(seq, (int) size);
    }

    public void add(long id, Sequence sequence) {
        maxNodeId = Math.max(maxNodeId, id);
        if (sequence.length() <= FEW_NUCLEOTIDES_LIMIT) {
            fewNps.get(sequence).addLong(id);
        } else if (sequence.getType() == SequenceType.SHORT_KNOWN) {
            SimpleNodeHandle node = new SimpleNodeHandle(id);
            var ns = new NodeSequence(node, sequence);
            nodeToShortSequenceMap.add(ns);
        } else if (sequence.getType() == SequenceType.SHORT_AMBIGUOUS) {
            SimpleNodeHandle node = new SimpleNodeHandle(id);
            var ns = new NodeSequence(node, sequence);
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
        LongIterator nlsmli = nodeToLongSequencePositionMap
                .keysView()
                .longIterator();
        var nlsm = fromLongIterator(nlsmli);
        List<OfLong> li = new ArrayList<>();

        for (Roaring64Bitmap r : fewNps.values()) {
            li.add(iteratorFromBitMap(r));
        }
        li.add(nssm);
        li.add(nlsm);
        return new CollectingOfLong(li.iterator());
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
