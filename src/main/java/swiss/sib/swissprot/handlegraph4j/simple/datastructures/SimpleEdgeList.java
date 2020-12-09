/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import me.lemire.integercompression.IntCompressor;
import me.lemire.integercompression.differential.IntegratedIntCompressor;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleEdgeHandle;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class SimpleEdgeList {

    protected static final int CHUNK_SIZE = 32 * 1024;
    private final ArrayList<Chunk> chunks;
    private BasicChunk current;

    public SimpleEdgeList() {
        this.chunks = new ArrayList<>();
    }

    public void add(SimpleEdgeHandle eh) {
        add(eh.left().id(), eh.right().id());
    }

    public void add(long left, long right) {

        if (current == null) {
            current = new BasicChunk();
        } else if (current.isFull()) {
            current.sort();
            if (CompressedChunck.canCompress(current)) {
                chunks.add(new CompressedChunck(current));
            } else {
                chunks.add(current);
            }
            current = new BasicChunk();
        }
        current.add(left, right);
    }

    public void trimAndSort() {
        if (current != null) {
            chunks.add(current);
            current = null;
        }
        sort();
    }

    public Stream<SimpleEdgeHandle> stream() {
        return chunks.stream().flatMap(Chunk::stream);
    }

    private void sort() {
        for (Chunk c : chunks) {
            c.sort();
        }
        chunks.sort((l, r)
                -> Long.compare(
                        l.first().left().id(),
                        r.first().left().id()
                ));
    }

    public Stream<SimpleEdgeHandle> streamToLeft(SimpleNodeHandle left) {
        return chunks.stream()
                .filter(c -> {
                    return left.id() >= c.first().left().id();
                })
                .filter(c -> {
                    return left.id() <= c.last().left().id();
                })
                .flatMap(Chunk::stream)
                .filter(e -> {
                    return e.left().id() == left.id();
                }
                );
    }

    private static int binarySearch0(long[] a, int fromIndex, int toIndex,
            long key) {
        int low = fromIndex;
        int high = toIndex - 2;

        while (low <= high) {
            int mid = (low + high) >>> 2;
            long midVal = a[mid];

            if (midVal < key) {
                low = mid + 2;
            } else if (midVal > key) {
                high = mid - 2;
            } else {
                return mid; // key found
            }
        }
        return -(low + 2);  // key not found.
    }

    public Stream<SimpleEdgeHandle> streamToRight(SimpleNodeHandle right) {
        return stream().filter(e -> e.right().equals(right));

    }

    private static class EdgeHandleIteratorImpl implements Iterator<SimpleEdgeHandle> {

        private int cursor = 0;
        private final int max;
        private final long[] lefts;
        private final long[] rights;

        public EdgeHandleIteratorImpl(int start, int max, long[] left, long[] right) {
            this.cursor = start;
            this.max = max;
            this.lefts = left;
            this.rights = right;
        }

        @Override
        public boolean hasNext() {
            return cursor < max;
        }

        @Override
        public SimpleEdgeHandle next() {
            SimpleEdgeHandle next = new SimpleEdgeHandle(lefts[cursor], rights[cursor]);
            cursor++;
            return next;
        }
    }

    private interface Chunk {

        public boolean isFull();

        public void sort();

        public Stream<SimpleEdgeHandle> stream();

        SimpleEdgeHandle first();

        SimpleEdgeHandle last();
    }

    private static class CompressedChunck implements Chunk {

        private final int firstLeft;
        private final int firstRight;
        private final int lastLeft;
        private final int lastRight;
        private final int[] compressedKeys;
        private final int[] compressedValues;
        private static final int MAX = 1 << 30;

        public static boolean canCompress(BasicChunk c) {
            if (c.size != CHUNK_SIZE) {
                return false;
            }
            long id = c.last().left().id();
            if (Math.abs(id) > MAX) {
                return false;
            }
            for (int i = 0; i < c.values.length; i++) {
                if (Math.abs(c.values[i]) > MAX) {
                    return false;
                }
            }
            return true;
        }

        public CompressedChunck(BasicChunk from) {
            IntegratedIntCompressor iic = new IntegratedIntCompressor();
            int[] rawKeys = new int[from.keys.length - 2];
            for (int i = 1; i < from.keys.length - 1; i++) {
                // We rotate right to make sure we have a positive number
                rawKeys[i - 1] = (int) rotateRight(from.keys[i]);
            }
            compressedKeys = iic.compress(rawKeys);
            int[] rawValues = new int[from.values.length - 2];
            for (int i = 1; i < from.values.length - 1; i++) {
                // We rotate right to make sure we have a positive number
                rawValues[i - 1] = (int) rotateRight(from.values[i]);
            }
            IntCompressor ic = new IntCompressor();
            compressedValues = ic.compress(rawValues);
            firstLeft = (int) rotateRight(from.first().left().id());
            firstRight = (int) rotateRight(from.first().right().id());
            lastLeft = (int) rotateRight(from.last().left().id());
            lastRight = (int) rotateRight(from.last().right().id());
        }

        private static long rotateRight(long id) {
            if (id >= 0) {
                return id << 1;
            } else {
                return (id << 1) | 1;
            }
        }

        private static long rotateLeft(long id) {
            if ((id & 1l) == 1l) {
                return -(id >>> 1);
            } else {
                return id >>> 1;
            }
        }

        @Override
        public boolean isFull() {
            return true;
        }

        @Override
        public void sort() {
            //already sorted;
        }

        @Override
        public Stream<SimpleEdgeHandle> stream() {
            var edgesIter = new Decompressing(compressedKeys, compressedValues);
            int characteristics = Spliterator.SIZED;
            var spliterator = Spliterators.spliterator(edgesIter, CHUNK_SIZE, characteristics);
            Stream<SimpleEdgeHandle> con = StreamSupport.stream(spliterator, false);
            return Stream.concat(Stream.concat(Stream.of(first()), con), Stream.of(last()));
        }

        @Override
        public SimpleEdgeHandle first() {
            return edge(firstLeft, firstRight);
        }

        private static SimpleEdgeHandle edge(int left, int right) {
            long leftId = rotateLeft((long) left);
            long rightId = rotateLeft((long) right);
            return new SimpleEdgeHandle(leftId, rightId);
        }

        @Override
        public SimpleEdgeHandle last() {
            return edge(lastLeft, lastRight);
        }

        private static class Decompressing implements Iterator<SimpleEdgeHandle> {

            private int cursor = 0;
            private final int[] lefts;
            private final int[] rights;

            public Decompressing(int[] lefts, int[] rights) {
                this.lefts = new IntegratedIntCompressor().uncompress(lefts);
                this.rights = new IntCompressor().uncompress(rights);
            }

            @Override
            public boolean hasNext() {
                return cursor < lefts.length;
            }

            @Override
            public SimpleEdgeHandle next() {
                long leftId = rotateLeft(lefts[cursor]);
                long rightId = rotateLeft(rights[cursor]);
                SimpleEdgeHandle next = new SimpleEdgeHandle(leftId, rightId);
                cursor++;
                return next;
            }
        }
    }

    private static class BasicChunk implements Chunk {

        private long[] keys = new long[CHUNK_SIZE];
        private long[] values = new long[CHUNK_SIZE];
        private int size = 0;
        private boolean sorted = false;

        public BasicChunk() {
        }

        @Override
        public boolean isFull() {
            return size == CHUNK_SIZE;
        }

        @Override
        public void sort() {
            if (size > 1) {
                long[] sortedKeys = new long[size];
                long[] sortedValues = new long[size];
                Stream<SimpleEdgeHandle> stream = stream();
                List<SimpleEdgeHandle> list = stream
                        .collect(Collectors.toList());
                list.sort(new LeftThenRightOrder());
                Iterator<SimpleEdgeHandle> iterator = list
                        .iterator();
                int i = 0;
                while (iterator.hasNext()) {
                    SimpleEdgeHandle next = iterator.next();
                    sortedKeys[i] = next.left().id();
                    sortedValues[i] = next.right().id();
                    i++;
                }
                keys = sortedKeys;
                values = sortedValues;
            }
            sorted = true;
        }

        private void add(long left, long right) {
            keys[size] = left;
            values[size] = right;
            size++;
            sorted = false;
        }

        @Override
        public Stream<SimpleEdgeHandle> stream() {
            //start from zero do not early terminate
            var edgesIter = new EdgeHandleIteratorImpl(0, size, keys, values);
            int characteristics = Spliterator.SIZED;
            if (!sorted) {
                characteristics = 0;
            }
            var spliterator = Spliterators.spliterator(edgesIter, size, characteristics);
            return StreamSupport.stream(spliterator, false);
        }

        @Override
        public SimpleEdgeHandle first() {
            return new SimpleEdgeHandle(keys[0], values[0]);
        }

        @Override
        public SimpleEdgeHandle last() {
            return new SimpleEdgeHandle(keys[size - 1], values[size - 1]);
        }

        private void trim() {
            if (values.length != size) {
                values = Arrays.copyOf(values, size);
                keys = Arrays.copyOf(keys, size);
            }
        }

        private static class LeftThenRightOrder implements Comparator<SimpleEdgeHandle> {

            public LeftThenRightOrder() {
            }

            @Override
            public int compare(SimpleEdgeHandle l, SimpleEdgeHandle r) {
                int cl = Long.compare(l.left().id(), r.left().id());
                if (cl == 0) {
                    return Long.compare(l.right().id(), r.right().id());
                } else {
                    return cl;
                }
            }
        }
    }

}
