/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.from;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.filter;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.map;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.flatMap;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.empty;
import io.github.vgteam.handlegraph4j.iterators.CollectingOfLong;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.Spliterator.NONNULL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PrimitiveIterator.OfLong;
import java.util.PrimitiveIterator.OfInt;
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
 * @param <T> the type of object representated by this key value store
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

    public Iterator<T> iterator() {
        Iterator<Iterator<T>> iterator = chunks.stream()
                .map(Chunk::iterator)
                .iterator();
        var toFlatMap = map(iterator, AutoClosedIterator::from);
        return flatMap(toFlatMap);
    }

    public void trimAndSort() {
        if (current != null) {
            chunks.add(current);
            current = null;
        }
        sort();
    }

    public Stream<T> streamToLeft(long key) {
        int index = 0;
        if (chunks.size() > 1) {
            index = findFirstChunkThatMightMatch(key);
        }

        return chunks.stream()
                .skip(index)
                .filter(c -> {
                    long fetchedKey = getKey.apply(c.first());
                    return key >= fetchedKey;
                })
                .takeWhile(c -> {
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

    public AutoClosedIterator<T> iterateToLeft(long key) {
        if (chunks.isEmpty())
            return empty();
        int firstIndex = 0;
        int lastIndex = 1;
        if (chunks.size() > 1) {
            firstIndex = findFirstChunkThatMightMatch(key);
            lastIndex = lastFirstChunkThatMightMatch(firstIndex, key);
        }
        List<Chunk<T>> potential = chunks.subList(firstIndex, lastIndex);
        if (potential.isEmpty()) {
            return empty();
        }
        var from = from(potential.
                iterator()
        );
        var ensureKeyGreaterThanFirst = filter(from, c -> {
            long fetchedKey = c.firstKey();
            return key >= fetchedKey;
        });
        var ensureKeySmallerThanLast = filter(ensureKeyGreaterThanFirst, c -> {
            long fetchedKey = c.lastKey();
            return key >= fetchedKey;
        });
        var inChunkIters = map(ensureKeySmallerThanLast, Chunk::iterator);
        var closeables = map(inChunkIters, AutoClosedIterator::from);
        var fromKey = flatMap(closeables);
        var matchingKey = filter(fromKey, e -> {
            long fetchedKey = getKey.apply(e);
            return fetchedKey == key;
        });
        return matchingKey;
    }

    private int findFirstChunkThatMightMatch(long key) {
        int index = Collections.binarySearch(chunks,
                new SearchChunk<>(key),
                (l, r) -> Long.compare(l.firstKey(), r.firstKey()));
        if (index > 0) {
            //We might need to backtrack as we found a chunck in which
            //have the key, but chunks that are before this one might have the
            //key as well
            while (index > 0 && chunks.get(index - 1).firstKey() == key) {
                index--;
            }
        } else if (index < 0) {
            index = Math.abs(index + 1);
            //If the insertion spot is after the last chunk the 
            //content can only be in the last chunk
            if (index >= chunks.size()) {
                return chunks.size() - 1;
            }
            //Make sure that we do not need to go to an earlier chunk
            while (index > 0 && chunks.get(index).firstKey() > key) {
                index--;
            }
        }
        return index;
    }

    private int lastFirstChunkThatMightMatch(int index, long key) {
        index++;
        //Check if there are more chunks that might have the data
        while (index < chunks.size() - 1
                && chunks.get(index + 1).lastKey() <= key) {
            index++;
        }
        return index;
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
                .flatMapToLong(c -> c.streamKeys());
    }

    public OfLong keyIterator() {
        Iterator<OfLong> iter = chunks
                .stream()
                .map(Chunk::keyIterator)
                .iterator();
        return new CollectingOfLong(iter);
    }
    
    public OfLong valueIterator() {
        Iterator<OfLong> iter = chunks
                .stream()
                .map(Chunk::valueIterator)
                .iterator();
        return new CollectingOfLong(iter);
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

        public Iterator<T> iterator();

        public LongStream streamKeys();

        public OfLong keyIterator();
        
        public OfLong valueIterator();

        T first();

        T last();

        long firstKey();

        long lastKey();
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
        public Iterator<T> iterator() {
            return stream().iterator();
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
        public OfLong keyIterator() {
            int[] intkeys = new IntegratedIntCompressor().uncompress(compressedKeys);
            return new OfLong() {
                int cursor = 0;

                int currentInt() {
                    if (cursor == 0) {
                        return firstKey;
                    } else if (cursor == intkeys.length) {
                        return lastKey;
                    } else {
                        int intkey = intkeys[cursor - 1];
                        return intkey;
                    }
                }

                @Override
                public long nextLong() {
                    long rotateLeft = rotateLeft(currentInt());
                    cursor++;
                    return rotateLeft;
                }

                @Override
                public boolean hasNext() {
                    return cursor < intkeys.length + 2;
                }
            };
        }
        
         @Override
        public OfLong valueIterator() {
            int[] intValues = new IntCompressor().uncompress(compressedKeys);
            return new OfLong() {
                int cursor = 0;

                int currentInt() {
                    if (cursor == 0) {
                        return firstValue;
                    } else if (cursor == intValues.length) {
                        return lastValue;
                    } else {
                        int intkey = intValues[cursor - 1];
                        return intkey;
                    }
                }

                @Override
                public long nextLong() {
                    long rotateLeft = rotateLeft(currentInt());
                    cursor++;
                    return rotateLeft;
                }

                @Override
                public boolean hasNext() {
                    return cursor < intValues.length + 2;
                }
            };
        }

        @Override
        public T first() {
            return reconstruct(firstKey, firstValue, reconstructor);
        }

        private T reconstruct(int left,
                int right,
                BiFunction<Long, Long, T> reconstructor) {
            long leftId = rotateLeft(left);
            long rightId = rotateLeft(right);
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

        @Override
        public long firstKey() {
            return rotateLeft(firstKey);
        }

        @Override
        public long lastKey() {
            return rotateLeft(lastKey);
        }

        private static class Decompressing<T> implements Iterator<T> {

            private int cursor = 0;
            private final int[] keys;
            private final int[] values;
            private final BiFunction<Long, Long, T> reconstructor;

            public Decompressing(int[] lefts,
                    int[] rights,
                    BiFunction<Long, Long, T> reconstructor) {
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
        }

        private void add(T t) {
            keys[size] = getKey.apply(t);
            values[size] = getValue.apply(t);
            size++;
        }

        @Override
        public Stream<T> stream() {
            //start from zero do not early terminate
            var edgesIter = new BasicChunkIterator(size, keys, values);
            var spliterator = spliteratorUnknownSize(edgesIter, NONNULL);
            return StreamSupport.stream(spliterator, false);
        }

        @Override
        public Iterator<T> iterator() {
            return stream().iterator();
        }

        @Override
        public OfLong keyIterator() {
            return new OfLong() {
                int cursor = 0;

                @Override
                public long nextLong() {
                    return keys[cursor++];
                }

                @Override
                public boolean hasNext() {
                    return cursor < keys.length;
                }
            };
        }

        @Override
        public OfLong valueIterator() {
            return new OfLong() {
                int cursor = 0;

                @Override
                public long nextLong() {
                    return values[cursor++];
                }

                @Override
                public boolean hasNext() {
                    return cursor < values.length;
                }
            };
        }
        
        @Override
        public T first() {
            return reconstructor.apply(keys[0], values[0]);
        }

        OfInt indexesOfKey(long key) {
            return new OfInt() {
                int start = skipToKey(key);

                @Override
                public int nextInt() {
                    return start++;
                }

                @Override
                public boolean hasNext() {
                    if (start < keys.length - 1) {
                        if (keys[start + 1] == key) {
                            return true;
                        }
                    }
                    return false;
                }
            };
        }

        private int skipToKey(long key) {
            int index = Arrays.binarySearch(keys, key);
            while (index > 0 && keys[index - 1] == key) {
                index--;
            }
            return index;
        }

        @Override
        public long firstKey() {
            return keys[0];
        }

        @Override
        public T last() {
            return reconstructor.apply(keys[size - 1], values[size - 1]);
        }

        @Override
        public long lastKey() {
            return keys[size - 1];
        }

        @Override
        public LongStream streamKeys() {
            return Arrays.stream(keys);
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

    private static class SearchChunk<T> implements Chunk<T> {

        private static final String NOT_SUPPORTED
                = "Not supported, this is just for the binary search.";
        private final long key;

        public SearchChunk(long key) {
            this.key = key;
        }

        @Override
        public long size() {
            throw new UnsupportedOperationException(NOT_SUPPORTED);
        }

        @Override
        public boolean isFull() {
            throw new UnsupportedOperationException(NOT_SUPPORTED);
        }

        @Override
        public void sort() {
            throw new UnsupportedOperationException(NOT_SUPPORTED);
        }

        @Override
        public Stream<T> stream() {
            throw new UnsupportedOperationException(NOT_SUPPORTED);
        }

        @Override
        public LongStream streamKeys() {
            throw new UnsupportedOperationException(NOT_SUPPORTED);
        }

        @Override
        public T first() {
            throw new UnsupportedOperationException(NOT_SUPPORTED);
        }

        @Override
        public T last() {
            throw new UnsupportedOperationException(NOT_SUPPORTED);
        }

        @Override
        public OfLong keyIterator() {
            throw new UnsupportedOperationException(NOT_SUPPORTED);
        }

        @Override
        public OfLong valueIterator() {
            throw new UnsupportedOperationException(NOT_SUPPORTED);
        }
        
        
        @Override
        public Iterator<T> iterator() {
            throw new UnsupportedOperationException(NOT_SUPPORTED);
        }

        @Override
        public long firstKey() {
            return key;
        }

        @Override
        public long lastKey() {
            throw new UnsupportedOperationException(NOT_SUPPORTED);
        }

    }

}
