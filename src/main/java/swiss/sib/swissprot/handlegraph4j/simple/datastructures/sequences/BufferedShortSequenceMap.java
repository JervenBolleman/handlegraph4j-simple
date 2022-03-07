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

    private static final int NOT_FOUND = -404;
	private final Map<Sequence, List<ImmutableRoaringBitmap>> fewNps;
    private final Map<Integer, List<RoaringBitmap>> lengthBitmaps = new HashMap<>();

    public BufferedShortSequenceMap(RandomAccessFile raf) throws IOException {
        int noOfsequences = raf.readInt();
        fewNps = new HashMap<>(noOfsequences);
        for (int i = 0; i < noOfsequences; i++) {
            Sequence st = readSequence(raf);

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
        
        fillLengthBitMaps(lengthBitmaps, fewNps);
    }

	static void fillLengthBitMaps(Map<Integer, List<RoaringBitmap>> lengthBitmaps, Map<Sequence, List<ImmutableRoaringBitmap>> fewNps) {
		for (Map.Entry<Sequence, List<ImmutableRoaringBitmap>> en:fewNps.entrySet()) {
        	Sequence seq = en.getKey();
        	if (! lengthBitmaps.containsKey(seq.length())) {
        		lengthBitmaps.put(seq.length(), new ArrayList<>());
        	}
        	List<RoaringBitmap> list = lengthBitmaps.get(seq.length());
        	List<ImmutableRoaringBitmap> value = en.getValue();
			for (int i=0;i<value.size();i++) {
				if (list.size() <= i) {
					list.add(new RoaringBitmap(value.get(i)));
				} else {
					list.get(i).or(new RoaringBitmap(value.get(i)));
				}
			}
        }
		lengthBitmaps.values().stream().flatMap(List::stream).forEach(RoaringBitmap::runOptimize);
	}

    public static void writeToDisk(ShortSequenceMap ssm, DataOutputStream raf) throws IOException, IllegalStateException {
        int noOfsequences = ssm.fewNps.size();
        raf.writeInt(noOfsequences);
        Iterator<Sequence> seqs = ssm.fewNps.keySet().iterator();
        for (int i = 0; i < noOfsequences; i++) {
            Sequence seq = seqs.next();
            sequenceToLong(seq, raf);
            List<RoaringBitmap> list = ssm.fewNps.get(seq);
            raf.writeInt(list.size());
            for (RoaringBitmap rb : list) {
            	if (! rb.hasRunCompression()) {
            		rb.runOptimize();
            	}
                int bits = rb.serializedSizeInBytes();
                raf.writeInt(bits);
                rb.serialize(raf);
            }
        }
    }

    private static void sequenceToLong(Sequence seq, DataOutputStream raf)
            throws IllegalStateException, IOException {
        raf.writeLong(seq.getType().code());
        if (seq instanceof ShortKnownSequence) {
            raf.writeLong(((ShortKnownSequence) seq).asLong());
        } else if (seq instanceof ShortAmbiguousSequence) {
            raf.writeLong(((ShortAmbiguousSequence) seq).asLong());
        } else {
            raf.writeInt(seq.length());
            for (int i = 0; i < seq.length(); i++) {
                raf.write(seq.byteAt(i));
            }
        }
    }

    private Sequence readSequence(RandomAccessFile raf)
            throws IllegalStateException, IOException {
        SequenceType st = SequenceType.fromLong(raf.readLong());
        switch (st) {
            case SHORT_AMBIGUOUS:
                return new ShortAmbiguousSequence(raf.readLong());

            case SHORT_KNOWN:
                return new ShortKnownSequence(raf.readLong());
            default: {
                int length = raf.readInt();
                byte[] bytes = new byte[length];
                for (int i =0;i<length;i++){
                    bytes[i]=raf.readByte();
                }
                return SequenceType.fromByteArray(bytes);
            }
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
                int raw = current.next();
                long id = (long) raw;
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

	public int getSequenceLength(long id) {
		int index = (int) (id >>> 32);
		RoaringBitmap toTest = new RoaringBitmap();
		toTest.add((int) id); // Drop the leading part.
		toTest.runOptimize();
        for (Entry<Integer, List<RoaringBitmap>> en : lengthBitmaps.entrySet()) {
            if (en.getValue().size() > index) {
                RoaringBitmap rb = en.getValue().get(index);
                if (RoaringBitmap.andCardinality(rb, toTest) > 0) {
                	return en.getKey();
                }
            }
        }
        return NOT_FOUND;
	}
}
