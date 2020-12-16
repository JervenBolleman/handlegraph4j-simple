/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures.chunks;

import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Iterator;
import java.util.PrimitiveIterator;
import swiss.sib.swissprot.handlegraph4j.simple.functions.LongLongToObj;
import swiss.sib.swissprot.handlegraph4j.simple.functions.ToLong;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class BasicBufferedChunk<T> implements Chunk<T> {

    final ByteBuffer buffer;
    private final LongLongToObj<T> reconstructor;

    public BasicBufferedChunk(BasicChunk<T> from) {
        this.reconstructor = from.reconstructor;
        long[] ar = new long[(int) (2 * from.size()) + 1];
        ar[0] = from.size();
        System.arraycopy(from.keys, 0, ar, 1, from.keys.length);
        System.arraycopy(from.values, 0, ar, 1 + from.keys.length, from.values.length);
        buffer = ByteBuffer.wrap(new byte[ar.length * Long.BYTES]);
        for (int i = 0; i < ar.length; i++) {
            buffer.putLong(ar[i]);
        }
    }

    public BasicBufferedChunk(ByteBuffer buffer, LongLongToObj<T> reconstructor) {
        this.buffer = buffer;
        this.reconstructor = reconstructor;
    }

    @Override
    public boolean isFull() {
        return true;
    }

    @Override
    public void sort() {
        //already sorted
    }

    @Override
    public Iterator<T> iterator(int start) {
        return new Iterator<T>() {
            int cursor = start;

            @Override
            public boolean hasNext() {
                return cursor < size();
            }

            @Override
            public T next() {
                return reconstructor.apply(getKey(cursor), getValue(cursor++));
            }
        };
    }

    @Override
    public PrimitiveIterator.OfLong keyIterator() {
        return new PrimitiveIterator.OfLong() {
            int cursor = 0;

            @Override
            public long nextLong() {
                return getKey(cursor++);
            }

            @Override
            public boolean hasNext() {
                return cursor < size();
            }
        };
    }

    @Override
    public PrimitiveIterator.OfLong valueIterator() {
        return new PrimitiveIterator.OfLong() {
            int cursor = 0;

            @Override
            public long nextLong() {
                return getValue(cursor++);
            }

            @Override
            public boolean hasNext() {
                return cursor < size();
            }
        };
    }

    @Override
    public T first() {
        return reconstructor.apply(getKey(0), getValue(0));
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
                if (start >= 0 && start < size()) {
                    if (getKey(start) == key) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private int skipToKey(long key) {
        int index = binarySearch(buffer, Long.BYTES, Long.BYTES + (Long.BYTES * (int) size()), key);
        while (index > 1 && getKey(index - 1) == key) {
            index--;
        }
        return index - 1; // compensate for the offset
    }

    private static int binarySearch(ByteBuffer lb, int fromIndex, int toIndex,
            long key) {
        int low = fromIndex;
        int high = toIndex - Long.BYTES;

        while (low <= high) {
            int mid = (low + high) >>> Long.BYTES;
            long midVal = lb.getLong(mid);

            if (midVal < key) {
                low = mid + Long.BYTES;
            } else if (midVal > key) {
                high = mid - Long.BYTES;
            } else {
                return mid; // found
            }
        }
        return -(low + 1);  // not found. (negative +1) as java binary search
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
                long key = getKey(index);
                long value = getValue(index);
                return reconstructor.apply(key, value);
            }
        });
    }

    private long getKey(int index) {
        int offset = (index * Long.BYTES) + Long.BYTES;
        return buffer.getLong(offset);
    }

    private long getValue(int index) {
        int offset = ((index + ((int)size())) * Long.BYTES) + Long.BYTES;
        return buffer.getLong(offset);
    }

    @Override
    public long firstKey() {
        return buffer.getLong(Long.BYTES);
    }

    @Override
    public T last() {
        return reconstructor.apply(getKey((int) size() - 1), getValue((int) size() - 1));
    }

    @Override
    public long lastKey() {
        return getKey((int) size() - 1);
    }

    @Override
    public long size() {
        return buffer.getLong(0);
    }

    @Override
    public void toStream(DataOutputStream stream) throws IOException {
        stream.writeInt(buffer.limit());
        if (buffer.hasArray()) {
            stream.write(buffer.array());
        } else {
            for (int i = 0; i < buffer.limit(); i++) {
                stream.write(buffer.get(i));
            }
        }
    }

    @Override
    public Type getType() {
        return Type.BASIC_BUFFERED;
    }
}
