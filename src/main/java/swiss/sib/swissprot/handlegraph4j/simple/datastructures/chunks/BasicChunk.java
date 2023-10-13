/**
 * Copyright (c) 2020, SIB Swiss Institute of Bioinformatics
 * and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures.chunks;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PrimitiveIterator;

import io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.LongLongSpinalList;
import swiss.sib.swissprot.handlegraph4j.simple.functions.LongLongToObj;
import swiss.sib.swissprot.handlegraph4j.simple.functions.ToLong;

/**
 *
 @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 */
public class BasicChunk<T> implements Chunk<T> {

    long[] keys = new long[LongLongSpinalList.CHUNK_SIZE];
    long[] values = new long[LongLongSpinalList.CHUNK_SIZE];
    int size = 0;
    final LongLongToObj<T> reconstructor;
    final ToLong<T> getKey;
    final ToLong<T> getValue;
    final Comparator<T> comparator;

    public BasicChunk(LongLongToObj<T> reconstructor, ToLong<T> getKey, ToLong<T> getValue, Comparator<T> comparator) {
        this.reconstructor = reconstructor;
        this.getKey = getKey;
        this.getValue = getValue;
        this.comparator = comparator;
    }

    @Override
    public boolean isFull() {
        return size == LongLongSpinalList.CHUNK_SIZE;
    }

    @Override
    public void sort() {
        if (size > 1) {
            long[] sortedKeys = new long[size];
            long[] sortedValues = new long[size];
            List<T> toSort = new ArrayList<>(size);
            Iterator<T> iterator = iterator();
            while (iterator.hasNext()) {
                toSort.add(iterator.next());
            }
            toSort.sort(comparator);
            iterator = toSort.iterator();
            int i = 0;
            while (iterator.hasNext()) {
                T next = iterator.next();
                sortedKeys[i] = getKey.apply(next);
                sortedValues[i] = getValue.apply(next);
                i++;
            }
            keys = sortedKeys;
            values = sortedValues;
        } else if (size  == 1) {
            long key = keys[0];
            long value = values[0];
            keys = new long[]{key};
            values = new long[]{value};
        }
    }

    public void add(T t) {
        keys[size] = getKey.apply(t);
        values[size] = getValue.apply(t);
        size++;
    }

    @Override
    public Iterator<T> iterator() {
        return iterator(0);
    }

    @Override
    public Iterator<T> iterator(int start) {
        return new Iterator<T>() {
            int cursor = start;

            @Override
            public boolean hasNext() {
                return cursor < size;
            }

            @Override
            public T next() {
                return reconstructor.apply(keys[cursor], values[cursor++]);
            }
        };
    }

    @Override
    public PrimitiveIterator.OfLong keyIterator() {
        return new PrimitiveIterator.OfLong() {
            int cursor = 0;

            @Override
            public long nextLong() {
                return keys[cursor++];
            }

            @Override
            public boolean hasNext() {
                return cursor < size;
            }
        };
    }

    @Override
    public PrimitiveIterator.OfLong valueIterator() {
        return new PrimitiveIterator.OfLong() {
            int cursor = 0;

            @Override
            public long nextLong() {
                return values[cursor++];
            }

            @Override
            public boolean hasNext() {
                return cursor < size;
            }
        };
    }

    @Override
    public T first() {
        return reconstructor.apply(keys[0], values[0]);
    }

    PrimitiveIterator.OfInt indexesOfKey(long key) {
        return new PrimitiveIterator.OfInt() {
            int start = skipToKey(key);

            @Override
            public int nextInt() {
                return start++;
            }

            @Override
            public boolean hasNext() {
                if (start >= 0 && start < keys.length) {
                    if (keys[start] == key) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private int skipToKey(long key) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        while (index > 1 && keys[index - 1] == key) {
            index--;
        }
        return index;
    }

    @Override
    public AutoClosedIterator<T> fromKey(long key, ToLong<T> getKey) {
        PrimitiveIterator.OfInt i = indexesOfKey(key);
        return AutoClosedIterator.from(new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return i.hasNext();
            }

            @Override
            public T next() {
                int index = i.nextInt();
                long key = keys[index];
                long value = values[index];
                return reconstructor.apply(key, value);
            }
        });
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
    public long size() {
        return size;
    }

    @Override
    public void toStream(DataOutputStream stream) throws IOException {
        sort();
        new BasicBufferedChunk<>(this).toStream(stream);
    }

    @Override
    public Type getType() {
        return Type.BASIC;
    }
}
