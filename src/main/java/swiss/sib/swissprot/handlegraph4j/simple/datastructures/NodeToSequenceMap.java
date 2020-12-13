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
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.of;
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
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Predicate;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.tuple.primitive.LongIntPair;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.LongIntHashMap;
import org.roaringbitmap.longlong.Roaring64Bitmap;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.LongLongSpinalList.ToLong;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class NodeToSequenceMap {

    private final Map<Sequence, Roaring64Bitmap> fewNps = new HashMap<>();
    private final LongLongSpinalList<NodeSequence<SimpleNodeHandle>> nodeToShortSequenceMap;
    private final LongIntHashMap nodeToLongSequenceOffsetMap;
    private final LongArrayList longSequenceLinearLayout;
    private long maxNodeId;
    private static final int FEW_NUCLEOTIDES_LIMIT = 3;

    private static int compareNodeSequence(NodeSequence o1, NodeSequence o2) {
        return Long.compare(o1.node().id(), o2.node().id());
    }

    private static long getNodeId(NodeSequence<SimpleNodeHandle> ns) {
        return ns.node().id();
    }

    private static long getSequenceAsLong(NodeSequence<SimpleNodeHandle> ns) {
        return sequenceAsLong(ns.sequence());
    }

    private static NodeSequence<SimpleNodeHandle> reconstruct(long key, long value) {
        var node = new SimpleNodeHandle(key);
        return new NodeSequence<>(node, sequenceFromEncodedLong(value));
    }

    public NodeToSequenceMap() {
        ToLong<NodeSequence<SimpleNodeHandle>> gn = NodeToSequenceMap::getNodeId;
        ToLong<NodeSequence<SimpleNodeHandle>> gs = NodeToSequenceMap::getSequenceAsLong;
        this.nodeToShortSequenceMap = new LongLongSpinalList<>(
                NodeToSequenceMap::reconstruct,
                gn,
                gs,
                NodeToSequenceMap::compareNodeSequence);
        this.nodeToLongSequenceOffsetMap = new LongIntHashMap();
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
            SingleNucleotideSequence seq = new SingleNucleotideSequence(b1);
            fewNps.put(seq, new Roaring64Bitmap());
            for (Character ch2 : Sequence.KNOWN_IUPAC_CODES) {
                byte b2 = (byte) ch2.charValue();
                byte[] ba = new byte[]{b1, b2};
                Sequence seq2 = SequenceType.fromByteArray(ba);
                fewNps.put(seq2, new Roaring64Bitmap());
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
                .mapToObj(SimpleNodeHandle::new);
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
        var mediums = nodesWithMediumKnownSequences();
        var iters = of(smalls, mediums, longs);
        return AutoClosedIterator.flatMap(iters);
    }

    private AutoClosedIterator<NodeSequence<SimpleNodeHandle>> nodesWithMediumKnownSequences() {
        return from(nodeToShortSequenceMap.iterator());
    }

    private class LongIntToSequence implements Function<LongIntPair, NodeSequence<SimpleNodeHandle>> {

        @Override
        public NodeSequence<SimpleNodeHandle> apply(LongIntPair p) {
            var node = new SimpleNodeHandle(p.getOne());
            var seq = getLongSequence(p.getTwo());
            return new NodeSequence<>(node, seq);
        }
    }

    private static class AttachNodeToSequence
            implements LongFunction<NodeSequence<SimpleNodeHandle>> {

        private final Sequence seq;

        public AttachNodeToSequence(Sequence seq) {
            this.seq = seq;
        }

        @Override
        public NodeSequence<SimpleNodeHandle> apply(long id) {
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
            return map(iteratorFromBitMap(b), new AttachNodeToSequence(s));
        }

    }

    private AutoClosedIterator<NodeSequence<SimpleNodeHandle>> nodesWithLongSequences() {
        var nodeOffset = nodeToLongSequenceOffsetMap.keyValuesView().iterator();
        var longToOffsetView = from(nodeOffset);

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
            LongFunction<SimpleNodeHandle> name = SimpleNodeHandle::new;
            return map(asLong, name);
        }
        if (s.getType() == SequenceType.LONG_VIA_ID) {
            var nAndSeq = nodesWithLongSequences();
            var filter = filter(nAndSeq, ns -> s.equals(ns.sequence()));
            return map(filter, NodeSequence::node);
        } else {
            long sl = sequenceAsLong(s);
            var from = from(nodeToShortSequenceMap.iterator());
            Predicate<NodeSequence<SimpleNodeHandle>> filter = (ns) -> sequenceAsLong(ns.sequence()) == sl;
            return map(filter(from, filter), NodeSequence::node);
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

        if (nodeToLongSequenceOffsetMap.containsKey(handle.id())) {
            int offset = nodeToLongSequenceOffsetMap.get(handle.id());
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

    private LongSequence getLongSequence(int offset) {
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
            nodeToLongSequenceOffsetMap.put(id, at);
        }
    }

    public void trim() {
        trimFewNps();
        nodeToLongSequenceOffsetMap.compact();
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

        if (nodeToShortSequenceMap.isEmpty() && nodeToLongSequenceOffsetMap.isEmpty()) {
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
        LongIterator nlsmli = nodeToLongSequenceOffsetMap
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

    public long count() {
        long sumSNps = fewNps.values()
                .stream()
                .mapToLong(Roaring64Bitmap::getLongCardinality)
                .sum();
        return sumSNps + nodeToShortSequenceMap.size() + nodeToLongSequenceOffsetMap.size();
    }
}
