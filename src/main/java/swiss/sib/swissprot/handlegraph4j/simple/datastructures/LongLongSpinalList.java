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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import me.lemire.integercompression.IntCompressor;
import me.lemire.integercompression.differential.IntegratedIntCompressor;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class LongLongSpinalList<T> {

    protected static final int CHUNK_SIZE = 32 * 1024;
    private final BiFunction<Long, Long, T> reconstructor;
    private final Function<T, Long> getKey;
    private final Function<T, Long> getValue;
    private final Comparator<T> comparator;
    private final ArrayList<Chunk<T>> chunks = new ArrayList<>();
    private BasicChunk<T> current;

    public LongLongSpinalList(BiFunction<Long, Long, T> reconstructor,
            Function<T, Long> getKey,
            Function<T, Long> getValue,
            Comparator<T> comparator) {
        this.reconstructor = reconstructor;
        this.getKey = getKey;
        this.getValue = getValue;
        this.comparator = comparator;
    }

    public Stream<T> stream() {
        return chunks.stream()
                .flatMap(Chunk::stream);
    }

    public void trimAndSort() {
        if (current != null) {
            chunks.add(current);
            current = null;
        }
        sort();
    }

    public Stream<T> streamToLeft(long key) {
        return chunks.stream()
                .filter(c -> {
                    long fetchedKey = getKey.apply(c.first());
                    return key >= fetchedKey;
                })
                .filter(c -> {
                    long fetchedKey = getKey.apply(c.last());
                    return key <= fetchedKey;
                })
                .flatMap(Chunk::stream)
                .filter(e -> {
                    long fetchedKey = getKey.apply(e);
                    return fetchedKey == key;
                }
                );
    }

    void sort() {
        for (Chunk<T> c : chunks) {
            c.sort();
        }
        chunks.sort((l, r)
                -> Long.compare(
                        getKey.apply(l.first()),
                        getKey.apply(r.first())
                ));
    }

    public void add(T t) {

        if (current == null) {
            current = new BasicChunk<>(reconstructor,
                    getKey,
                    getValue,
                    comparator);
        } else if (current.isFull()) {
            current.sort();
            if (CompressedChunck.<T>canCompress(current, getKey, getValue)) {
                CompressedChunck<T> cc = new CompressedChunck<>(current,
                        reconstructor,
                        getKey,
                        getValue);
                chunks.add(cc);
            } else {
                chunks.add(current);
            }
            current = new BasicChunk<>(reconstructor,
                    getKey,
                    getValue,
                    comparator);
        }
        current.add(t);
    }

    boolean isEmpty() {
        if (chunks.isEmpty() && current == null) {
            return true;
        } else if (current != null) {
            return current.size == 0;
        } else {
            for (Chunk<T> c : chunks) {
                if (c.first() != null) {
                    return false;
                }
            }
        }
        return true;
    }

    public LongStream keyStream() {
        return chunks.stream()
                .flatMapToLong(c ->c.streamKeys());
    }

    public long size() {
        return chunks.stream()
                .mapToLong(c -> c.size())
                .sum();
    }

    private interface Chunk<T> {

        public long size();

        public boolean isFull();

        public void sort();

        public Stream<T> stream();
        
        public LongStream streamKeys();

        T first();

        T last();
    }

    private static class CompressedChunck<T> implements Chunk<T> {

        private final BiFunction<Long, Long, T> reconstructor;
        private final int firstKey;
        private final int firstValue;
        private final int lastKey;
        private final int lastValue;
        private final int[] compressedKeys;
        private final int[] compressedValues;
        private static final int MAX = 1 << 30;

        public CompressedChunck(BasicChunk<T> from,
                BiFunction<Long, Long, T> reconstructor,
                Function<T, Long> getKey,
                Function<T, Long> getValue) {
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
            firstKey = (int) rotateRight(getKey.apply(from.first()));
            firstValue = (int) rotateRight(getValue.apply(from.first()));
            lastKey = (int) rotateRight(getKey.apply(from.last()));
            lastValue = (int) rotateRight(getValue.apply(from.last()));
            this.reconstructor = reconstructor;
        }

        public static <T> boolean canCompress(BasicChunk<T> c,
                Function<T, Long> getKey,
                Function<T, Long> getValue) {
            if (c.size != CHUNK_SIZE) {
                return false;
            }
            long id = getKey.apply(c.last());
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
        public Stream<T> stream() {
            var edgesIter = new Decompressing(compressedKeys, compressedValues, reconstructor);
            int characteristics = Spliterator.SIZED;
            var spliterator = Spliterators.spliterator(edgesIter, CHUNK_SIZE, characteristics);
            Stream<T> con = StreamSupport.stream(spliterator, false);
            return Stream.concat(Stream.concat(Stream.of(first()), con), Stream.of(last()));
        }
        
        @Override
        public LongStream streamKeys() {
            int[] intkeys = new IntegratedIntCompressor().uncompress(compressedKeys);
            IntStream rawkeys = Arrays.stream(intkeys);
            IntStream start = IntStream.of(firstKey);
            IntStream middle = IntStream.concat(start, rawkeys);
            IntStream end = IntStream.of(lastKey);
                    
            return IntStream.concat(middle, end)
                    .mapToLong(CompressedChunck::rotateLeft);
        }

        @Override
        public T first() {
            return reconstruct(firstKey, firstValue, reconstructor);
        }

        private T reconstruct(int left, int right, BiFunction<Long, Long, T> reconstructor) {
            long leftId = rotateLeft((long) left);
            long rightId = rotateLeft((long) right);
            return reconstructor.apply(leftId, rightId);
        }

        @Override
        public T last() {
            return reconstruct(lastKey, lastValue, reconstructor);
        }

        @Override
        public long size() {
            return CHUNK_SIZE;
        }

        private static class Decompressing<T> implements Iterator<T> {

            private int cursor = 0;
            private final int[] keys;
            private final int[] values;
            private final BiFunction<Long, Long, T> reconstructor;

            public Decompressing(int[] lefts, int[] rights, BiFunction<Long, Long, T> reconstructor) {
                this.keys = new IntegratedIntCompressor().uncompress(lefts);
                this.values = new IntCompressor().uncompress(rights);
                this.reconstructor = reconstructor;
            }

            @Override
            public boolean hasNext() {
                return cursor < keys.length;
            }

            @Override
            public T next() {
                long key = rotateLeft(keys[cursor]);
                long value = rotateLeft(values[cursor]);
                T next = reconstructor.apply(key, value);
                cursor++;
                return next;
            }
        }
    }

    private static class BasicChunk<T> implements Chunk<T> {

        private long[] keys = new long[CHUNK_SIZE];
        private long[] values = new long[CHUNK_SIZE];
        private int size = 0;
        private boolean sorted = false;
        private final BiFunction<Long, Long, T> reconstructor;
        private final Function<T, Long> getKey;
        private final Function<T, Long> getValue;
        private final Comparator<T> comparator;

        public BasicChunk(BiFunction<Long, Long, T> reconstructor,
                Function<T, Long> getKey,
                Function<T, Long> getValue,
                Comparator<T> comparator) {
            this.reconstructor = reconstructor;
            this.getKey = getKey;
            this.getValue = getValue;
            this.comparator = comparator;
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
                Stream<T> stream = stream();
                List<T> list = stream
                        .collect(Collectors.toList());
                list.sort(comparator);
                Iterator<T> iterator = list
                        .iterator();
                int i = 0;
                while (iterator.hasNext()) {
                    T next = iterator.next();
                    sortedKeys[i] = getKey.apply(next);
                    sortedValues[i] = getValue.apply(next);
                    i++;
                }
                keys = sortedKeys;
                values = sortedValues;
            }
            sorted = true;
        }

        private void add(T t) {
            keys[size] = getKey.apply(t);
            values[size] = getValue.apply(t);
            size++;
            sorted = false;
        }

        @Override
        public Stream<T> stream() {
            //start from zero do not early terminate
            var edgesIter = new BasicChunkIterator(size, keys, values);
            int characteristics = Spliterator.SIZED | Spliterator.SUBSIZED
                    | Spliterator.NONNULL;
            if (!sorted) {
                characteristics = 0;
            }
            var spliterator = Spliterators.spliterator(edgesIter, size, characteristics);
            return StreamSupport.stream(spliterator, false);
        }

        @Override
        public T first() {
            return reconstructor.apply(keys[0], values[0]);
        }

        @Override
        public T last() {
            return reconstructor.apply(keys[size - 1], values[size - 1]);
        }

        @Override
        public LongStream streamKeys() {
            return Arrays.stream(keys);
        }

        private void trim() {
            if (values.length != size) {
                values = Arrays.copyOf(values, size);
                keys = Arrays.copyOf(keys, size);
            }
        }

        @Override
        public long size() {
            return size;
        }

        private class BasicChunkIterator implements Iterator<T> {

            private int cursor = 0;
            private final int max;
            private final long[] lefts;
            private final long[] rights;

            public BasicChunkIterator(int max, long[] left, long[] right) {
                this.max = max;
                this.lefts = left;
                this.rights = right;
            }

            @Override
            public boolean hasNext() {
                return cursor < max;
            }

            @Override
            public T next() {
                T next = reconstructor.apply(lefts[cursor], rights[cursor]);
                cursor++;
                return next;
            }
        }
    }
}
