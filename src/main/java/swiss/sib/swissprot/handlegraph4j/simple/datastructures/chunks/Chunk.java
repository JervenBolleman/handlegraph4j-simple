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
import java.util.Iterator;
import java.util.PrimitiveIterator;

import io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator;
import swiss.sib.swissprot.handlegraph4j.simple.functions.ToLong;

/**
 *
 @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 */
public sealed interface Chunk<T> permits BasicChunk, CompressedChunk, CompressedBufferedChunk, SearchChunk, BasicBufferedChunk {

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
        AutoClosedIterator<T> iterator = AutoClosedIterator.from(iterator());
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
