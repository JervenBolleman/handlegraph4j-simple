/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures.sequences;

import io.github.vgteam.handlegraph4j.NodeSequence;
import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.empty;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.flatMap;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.map;
import io.github.vgteam.handlegraph4j.iterators.CollectingOfLong;
import io.github.vgteam.handlegraph4j.sequences.Sequence;
import io.github.vgteam.handlegraph4j.sequences.SequenceType;
import io.github.vgteam.handlegraph4j.sequences.ShortAmbiguousSequence;
import io.github.vgteam.handlegraph4j.sequences.ShortKnownSequence;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PrimitiveIterator;
import java.util.PrimitiveIterator.OfLong;
import java.util.function.Function;
import java.util.function.LongFunction;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;
import org.roaringbitmap.buffer.ImmutableRoaringBitmap;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.BufferedNodeToSequenceMap;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class BufferedShortSequenceMap implements NodeSequenceMap {

    private final Map<Sequence, List<ImmutableRoaringBitmap>> fewNps = new HashMap<>();

    public BufferedShortSequenceMap(RandomAccessFile raf) throws IOException {
        int noOfsequences = raf.readInt();
        for (int i = 0; i < noOfsequences; i++) {
            long seqAsLong = raf.readLong();
            Sequence st = readSequence(seqAsLong);

            long maps = raf.readInt();
            List<ImmutableRoaringBitmap> list = new ArrayList<>();
            for (int j = 0; j < maps; j++) {
                int bits = raf.readInt();
                MappedByteBuffer map = raf.getChannel().map(MapMode.READ_ONLY,
                        raf.getFilePointer(),
                        bits);
                raf.seek(raf.getFilePointer() + bits);
                list.add(new ImmutableRoaringBitmap(map));
            }
            fewNps.put(st, list);
        }
    }

    public static void writeToDisk(ShortSequenceMap ssm, DataOutputStream raf) throws IOException, IllegalStateException {
        int noOfsequences = ssm.fewNps.size();
        raf.writeInt(noOfsequences);
        Iterator<Sequence> seqs = ssm.fewNps.keySet().iterator();
        for (int i = 0; i < noOfsequences; i++) {
            Sequence seq = seqs.next();
            long ls = sequenceToLong(seq);
            raf.writeLong(ls);
            List<RoaringBitmap> list = ssm.fewNps.get(seq);
//            long maps = raf.readInt();
            raf.writeInt(list.size());
            for (RoaringBitmap rb : list) {
                int bits = rb.serializedSizeInBytes();
                raf.writeInt(bits);
                rb.serialize(raf);
            }
        }
    }

    private static long sequenceToLong(Sequence seq) throws IllegalStateException {

        if (seq instanceof ShortKnownSequence) {
            return ((ShortKnownSequence) seq).asLong();
        } else if (seq instanceof ShortAmbiguousSequence) {
            return ((ShortAmbiguousSequence) seq).asLong();
        } else {
            throw new IllegalStateException();
        }
    }

    private Sequence readSequence(long seqAsLong) throws IllegalStateException {
        SequenceType st = SequenceType.fromLong(seqAsLong);
        switch (st) {
            case SHORT_AMBIGUOUS:
                return new ShortAmbiguousSequence(seqAsLong);

            case SHORT_KNOWN:
                return new ShortKnownSequence(seqAsLong);

            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public boolean containsSequence(Sequence s) {
        return fewNps.containsKey(s);
    }

    @Override
    public void add(long id, Sequence sequence) {
    }

    @Override
    public void trim() {

    }

    @Override
    public long size() {
        return fewNps.values()
                .stream()
                .flatMap(List::stream)
                .mapToLong(ImmutableRoaringBitmap::getLongCardinality)
                .sum();
    }

    @Override
    public int maxSequenceLength() {
        return fewNps.keySet()
                .stream()
                .mapToInt(Sequence::length)
                .max()
                .orElse(0);
    }

    @Override
    public AutoClosedIterator<SimpleNodeHandle> nodeWithSequences(Sequence s) {
        var get = fewNps.get(s);
        if (get.isEmpty()) {
            return empty();
        } else {
            PrimitiveIterator.OfLong nodeids = iteratorFromBitMaps(get);
            LongFunction<SimpleNodeHandle> name = SimpleNodeHandle::new;
            return map(nodeids, name);
        }
    }

    @Override
    public boolean isEmpty() {
        return fewNps.values()
                .stream()
                .flatMap(List::stream)
                .anyMatch(r -> !r.isEmpty());
    }

    private class SequenceAndRoaringBitMap implements
            Function<Map.Entry<Sequence, List<ImmutableRoaringBitmap>>, AutoClosedIterator<NodeSequence<SimpleNodeHandle>>> {

        @Override
        public AutoClosedIterator<NodeSequence<SimpleNodeHandle>> apply(Map.Entry<Sequence, List<ImmutableRoaringBitmap>> en) {
            Sequence s = en.getKey();
            List<ImmutableRoaringBitmap> bitmaps = en.getValue();
            if (bitmaps.isEmpty()) {
                return empty();
            }
            PrimitiveIterator.OfLong nodeids = iteratorFromBitMaps(bitmaps);
            return map(nodeids, new BufferedNodeToSequenceMap.AttachNodeToSequence(s));
        }
    }

    @Override
    public AutoClosedIterator<NodeSequence<SimpleNodeHandle>> nodeSequences() {
        var smalls = flatMap(map(fewNps.entrySet().iterator(), new SequenceAndRoaringBitMap()));
        return smalls;
    }

    @Override
    public OfLong nodeIds() {
        var lists = map(fewNps.entrySet().iterator(), Entry::getValue);
        var nodes = map(lists, BufferedShortSequenceMap::iteratorFromBitMaps);
        return new CollectingOfLong(nodes);
    }

    private static PrimitiveIterator.OfLong iteratorFromBitMaps(List<ImmutableRoaringBitmap> bitmaps) {
        PrimitiveIterator.OfLong nodeids = new PrimitiveIterator.OfLong() {
            long index = 0;
            PeekableIntIterator current = bitmaps.get(0).getIntIterator();

            @Override
            public long nextLong() {
                long id = (long) current.next();
                return id | (index << Integer.BYTES * 8);
            }

            @Override
            public boolean hasNext() {
                if (current.hasNext()) {
                    return true;
                }
                while (index < bitmaps.size() - 2 && !current.hasNext()) {
                    index++;
                    current = bitmaps.get((int) index).getIntIterator();
                    if (current.hasNext()) {
                        return true;
                    }
                }
                return false;
            }
        };
        return nodeids;
    }

    @Override
    public Sequence getSequence(long id) {
        int index = (int) (id >>> 32);
        for (Entry<Sequence, List<ImmutableRoaringBitmap>> en : fewNps.entrySet()) {
            if (en.getValue().size() > index) {
                ImmutableRoaringBitmap rb = en.getValue().get(index);
                if (rb.contains((int) id)) {
                    return en.getKey();
                }
            }
        }
        return null;
    }
}