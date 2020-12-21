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
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.Iterator;
import java.util.PrimitiveIterator.OfLong;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.list.primitive.LongList;
import org.eclipse.collections.api.tuple.primitive.LongIntPair;
import org.eclipse.collections.impl.map.mutable.primitive.LongIntHashMap;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class BufferedLongSequenceMap implements NodeSequenceMap {

    public static void writeToDisk(LongSequenceMap nodesWithLongSequences, DataOutputStream raf) throws IOException {

        writeSequences(raf, nodesWithLongSequences.longSequenceLinearLayout);
        writeOffsetMap(raf, nodesWithLongSequences.nodesWithLongSequences);
    }

    private final LongIntHashMap nodesWithLongSequences;
    private final LongBuffer longSequenceLinearLayout;

    public BufferedLongSequenceMap(RandomAccessFile raf) throws IOException {
        int size = raf.readInt() * Long.BYTES;
        long start = raf.getFilePointer();
        MappedByteBuffer map = raf.getChannel().map(MapMode.READ_ONLY, start, size);
        this.longSequenceLinearLayout = map.asLongBuffer();
        raf.seek(raf.getFilePointer() + map.limit());
        size = raf.readInt();
        this.nodesWithLongSequences = new LongIntHashMap(size);
        int max = size * (Long.BYTES + Integer.BYTES);
        int stepSize = (Long.BYTES + Integer.BYTES) * VALUES_PER_READ;
        int steps = max / stepSize;
        readInByteArrayBlocks(steps, stepSize, raf);
        readInLastElements(size, raf);
    }

    private void readInLastElements(int size, RandomAccessFile raf) throws IOException {
        for (int i = nodesWithLongSequences.size(); i < size; i++) {
            nodesWithLongSequences.put(raf.readLong(), raf.readInt());
        }
    }

    private void readInByteArrayBlocks(int steps, int stepSize, RandomAccessFile raf) throws IOException {
        for (int i = 0; i < steps; i ++) {
            byte[] temp = new byte[stepSize];
            int read = raf.read(temp);
            assert read != -1;
            assert read == stepSize;
            try ( ByteArrayInputStream bin = new ByteArrayInputStream(temp);  DataInputStream dis = new DataInputStream(bin)) {
                for (int j = 0; j < VALUES_PER_READ; j++) {
                    nodesWithLongSequences.put(dis.readLong(), dis.readInt());
                }
            }
        }
    }
    private static final int VALUES_PER_READ = 512;

    private static void writeOffsetMap(DataOutputStream raf, LongIntHashMap nodesWithLongSequences1) throws IOException {
        raf.writeInt(nodesWithLongSequences1.size());
        Iterator<LongIntPair> iterator = nodesWithLongSequences1.keyValuesView().iterator();
        while (iterator.hasNext()) {
            LongIntPair next = iterator.next();
            raf.writeLong(next.getOne());
            raf.writeInt(next.getTwo());
        }
    }

    private static void writeSequences(DataOutputStream raf, LongList linear) throws IOException {
        raf.writeInt(linear.size());
        LongIterator longIterator = linear.longIterator();
        while (longIterator.hasNext()) {
            raf.writeLong(longIterator.next());
        }
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
            seq[i] = longSequenceLinearLayout.get(offset + i + 2);
        }
        return new LongSequence(seq, (int) size);
    }

    @Override
    public void add(long id, Sequence sequence) {
//        int at = longSequenceLinearLayout.size();
//        int size = sequence.length();
//        long[] s = ((LongSequence) sequence).array();
//        int longs = s.length;
//        longSequenceLinearLayout.add(id);
//        long sizeAndLongs = (((long) size) << 32) | (long) longs;
//        longSequenceLinearLayout.add(sizeAndLongs);
//        for (int i = 0; i < longs; i++) {
//            longSequenceLinearLayout.add(s[i]);
//        }
//        nodesWithLongSequences.put(id, at);
    }

    @Override
    public void trim() {
        nodesWithLongSequences.compact();
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

    @Override
    public boolean containsSequence(Sequence s) {
        var iter = new LinearLongSequenceIterator(longSequenceLinearLayout);
        try ( var seqs = from(iter)) {
            try ( var from = filter(seqs, ns -> seqInNodeSeq(ns, s))) {
                return from.hasNext();
            }
        }
    }

    private static class LinearLongSequenceIterator
            implements Iterator<NodeSequence<SimpleNodeHandle>> {

        private final LongBuffer longSequenceLinearLayout;

        int offset = 0;

        private LinearLongSequenceIterator(LongBuffer longSequenceLinearLayout) {
            this.longSequenceLinearLayout = longSequenceLinearLayout;
        }

        @Override
        public boolean hasNext() {
            return offset < longSequenceLinearLayout.limit();
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

    @Override
    public int maxSequenceLength() {
        int max = 0;
        var iter = new LinearLongSequenceIterator(longSequenceLinearLayout);
        try ( var seqs = from(iter)) {
            while (seqs.hasNext()) {
                max = Math.max(0, seqs.next().sequence().length());
            }
        }
        return max;
    }

}
