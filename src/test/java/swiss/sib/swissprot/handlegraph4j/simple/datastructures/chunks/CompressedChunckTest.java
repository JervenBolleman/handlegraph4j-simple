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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PrimitiveIterator.OfLong;

import org.junit.jupiter.api.Test;

import io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator;
import swiss.sib.swissprot.handlegraph4j.simple.functions.LongLongToObj;
import swiss.sib.swissprot.handlegraph4j.simple.functions.ToLong;

/**
 *
 @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 */
public class CompressedChunckTest {

    private LongLongToObj<long[]> kva = (k, v) -> new long[]{k, v};
    private ToLong<long[]> gk = a -> a[0];
    private ToLong<long[]> gv = a -> a[1];
    private Comparator<long[]> comp = (a, b) -> Long.compare(a[0], b[0]);

    public CompressedChunckTest() {
    }

    /**
     * Test of canCompress method, of class CompressedChunk.
     */
    @Test
    public void testCanCompress() {
        int length = 1000;
        BasicChunk<long[]> basicChunk = createBasicChunk(kva, gk, gv, comp, length);

        assertTrue(CompressedChunk.canCompress(basicChunk, gk, gv));
    }

    private BasicChunk<long[]> createBasicChunk(LongLongToObj<long[]> kva, ToLong<long[]> gk, ToLong<long[]> gv, Comparator<long[]> comp, int length) {
        BasicChunk<long[]> basicChunk = new BasicChunk<>(kva, gk, gv, comp);
        for (int i = 0; i < length; i++) {
            basicChunk.add(new long[]{i, i});
        }
        basicChunk.sort();
        return basicChunk;
    }

    /**
     * Test of iterator method, of class CompressedChunk.
     */
    @Test
    public void testIterator() {
        int length = 1000;
        BasicChunk<long[]> basicChunk = createBasicChunk(kva, gk, gv, comp, length);
        int i = 0;
        var cc = new CompressedChunk<long[]>(basicChunk, kva, gk, gv);
        Iterator<long[]> iterator = cc.iterator();
        while (iterator.hasNext()) {
            long[] next = iterator.next();
            assertEquals(next[0], i);
            assertEquals(next[1], i);
            i++;
        }
        assertEquals(i, length);
    }

    /**
     * Test of keyIterator method, of class CompressedChunk.
     */
    @Test
    public void testKeyIterator_0args() {
        int length = 1000;
        BasicChunk<long[]> basicChunk = createBasicChunk(kva, gk, gv, comp, length);
        int i = 0;
        var cc = new CompressedChunk<long[]>(basicChunk, kva, gk, gv);
        OfLong iterator = cc.keyIterator();
        while (iterator.hasNext()) {
            assertEquals(iterator.next(), i);
            i++;
        }
        assertEquals(i, length);
    }

    /**
     * Test of valueIterator method, of class CompressedChunk.
     */
    @Test
    public void testValueIterator_0args() {
        int length = 1000;
        BasicChunk<long[]> basicChunk = createBasicChunk(kva, gk, gv, comp, length);
        int i = 0;
        var cc = new CompressedChunk<long[]>(basicChunk, kva, gk, gv);
        OfLong iterator = cc.valueIterator();
        while (iterator.hasNext()) {
            assertEquals(iterator.next(), i);
            i++;
        }
        assertEquals(i, length);
    }

    /**
     * Test of hasKey method, of class CompressedChunk.
     */
    @Test
    public void testHasKey() {
        int length = 1000;
        CompressedChunk<long[]> cc = createInstance(length);
        for (int i = 0; i < length; i++) {
            assertTrue(cc.hasKey(i));
        }
        assertFalse(cc.hasKey(length + 1));
    }

    /**
     * Test of fromKey method, of class CompressedChunk.
     */
    @Test
    public void testFromKey() {
        int length = 1000;
        CompressedChunk<long[]> cc = createInstance(length);

        try ( AutoClosedIterator<long[]> iter = cc.fromKey(500, gk)) {
            int i = 500;
            while (iter.hasNext()) {
                assertEquals(i, iter.next()[0]);
                assertEquals(i, iter.next()[1]);
                i++;
            }
            assertFalse(iter.hasNext());
        }

    }

    private CompressedChunk<long[]> createInstance(int length) {
        BasicChunk<long[]> basicChunk = createBasicChunk(kva, gk, gv, comp, length);
        var cc = new CompressedChunk<long[]>(basicChunk, kva, gk, gv);
        return cc;
    }

    /**
     * Test of first method, of class CompressedChunk.
     */
    @Test
    public void testFirst() {
        int length = 1000;
        CompressedChunk<long[]> cc = createInstance(length);
        long[] result = cc.first();
        assertArrayEquals(new long[]{0, 0}, result);
    }

    /**
     * Test of last method, of class CompressedChunk.
     */
    @Test
    public void testLast() {
        int length = 1000;
        CompressedChunk<long[]> cc = createInstance(length);
        long[] result = cc.last();
        assertArrayEquals(new long[]{999, 999}, result);
    }

    /**
     * Test of size method, of class CompressedChunk.
     */
    @Test
    public void testSize() {
        int length = 1000;
        CompressedChunk<long[]> cc = createInstance(length);
        long result = cc.size();
        assertEquals(length, result);
    }

    /**
     * Test of firstKey method, of class CompressedChunk.
     */
    @Test
    public void testFirstKey() {
        int length = 1000;
        CompressedChunk<long[]> cc = createInstance(length);
        long expResult = 0L;
        long result = cc.firstKey();
        assertEquals(expResult, result);
    }

    /**
     * Test of lastKey method, of class CompressedChunk.
     */
    @Test
    public void testLastKey() {
        int length = 1000;
        CompressedChunk<long[]> cc = createInstance(length);
        long expResult = 999L;
        long result = cc.lastKey();
        assertEquals(expResult, result);
    }

}
