package swiss.sib.swissprot.handlegraph4j.simple.datastructures.chunks;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.PrimitiveIterator;
import swiss.sib.swissprot.handlegraph4j.simple.functions.ToLong;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public interface Chunk<T> {

    public long size();

    public boolean isFull();

    public void sort();

    public default Iterator<T> iterator() {
        return iterator(0);
    }

    public Iterator<T> iterator(int start);

    public PrimitiveIterator.OfLong keyIterator();

    public PrimitiveIterator.OfLong valueIterator();

    T first();

    T last();

    long firstKey();

    long lastKey();

    public default boolean hasKey(long key) {
        if (key < firstKey()) {
            return false;
        }
        if (key > lastKey()) {
            return false;
        }
        java.util.PrimitiveIterator.OfLong keys = keyIterator();
        while (keys.hasNext()) {
            if (keys.nextLong() == key) {
                return true;
            }
        }
        return false;
    }

    public default AutoClosedIterator<T> fromKey(long key, ToLong<T> getKey) {
        if (key < firstKey()) {
            return AutoClosedIterator.empty();
        } else if (key > lastKey()) {
            return AutoClosedIterator.empty();
        }
        io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator<T> iterator = AutoClosedIterator.from(iterator());
        return AutoClosedIterator.terminate(iterator, next -> getKey.apply(next) == key);
    }

    public void toStream(DataOutputStream stream) throws IOException;

    public Type getType();

    enum Type {
        BASIC(1),
        BASIC_BUFFERED(2),
        COMPRESSED(3),
        COMPRESSED_BUFFERED(4),
        SORT(-1);

        public static Type fromCode(byte type) {
            for (Type t : values()) {
                if (t.getCode() == type) {
                    return t;
                }
            }
            return null;
        }
        private final byte code;

        private Type(int code) {
            this.code = (byte) code;
        }

        public byte getCode() {
            return code;
        }

    }
}
