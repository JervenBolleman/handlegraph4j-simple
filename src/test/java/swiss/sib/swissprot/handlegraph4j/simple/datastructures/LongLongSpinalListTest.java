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
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PrimitiveIterator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator;
import swiss.sib.swissprot.handlegraph4j.simple.functions.LongLongToObj;
import swiss.sib.swissprot.handlegraph4j.simple.functions.ToLong;

/**
 *
 @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 */
public class LongLongSpinalListTest {

    @TempDir
    File anotherTempDir;

    private final LongLongToObj<long[]> name = (k, v) -> new long[]{k, v};
    private final ToLong<long[]> gk = a -> a[0];
    private final ToLong<long[]> gv = a -> a[1];
    private final Comparator<long[]> comp = (a, b) -> Long.compare(a[0], b[0]);

    public LongLongSpinalListTest() {
    }

    /**
     * Test of iterator method, of class LongLongSpinalList.
     */
    @Test
    public void testIterator() {
        LongLongSpinalList<long[]> instance = newInstance();
        Iterator<long[]> result = instance.iterator();
        for (int i = 0; i < 10; i++) {
            assertTrue(result.hasNext());
            long[] next = result.next();
            assertEquals(i, next[0]);
            assertEquals(i, -next[1]);
        }
        assertFalse(result.hasNext());
    }

    private LongLongSpinalList<long[]> newInstance() {
        return newInstance(10);
    }

    private LongLongSpinalList<long[]> newInstance(int size) {

        LongLongSpinalList<long[]> instance = new LongLongSpinalList<>(name, gk, gv, comp);
        for (int i = 0; i < size; i++) {
            instance.add(new long[]{i, -i});
        }
        instance.trimAndSort();
        return instance;
    }

    /**
     * Test of iterateWithKey method, of class LongLongSpinalList.
     */
    @Test
    public void testIterateToLeft() {
        LongLongSpinalList<long[]> instance = newInstance();
        for (int i = 0; i < 10; i++) {
            try ( AutoClosedIterator<long[]> result = instance.iterateWithKey(i)) {
                assertTrue(result.hasNext());
                long[] next = result.next();
                assertEquals(i, next[0]);
                assertEquals(i, -next[1]);
                assertFalse(result.hasNext());
            }
        }
    }

    /**
     * Test of isEmpty method, of class LongLongSpinalList.
     */
    @Test
    public void testIsEmpty() {
        LongLongSpinalList<long[]> instance = newInstance();
        boolean expResult = false;
        boolean result = instance.isEmpty();
        assertEquals(expResult, result);
    }

    /**
     * Test of keyIterator method, of class LongLongSpinalList.
     */
    @Test
    public void testKeyIterator() {
        LongLongSpinalList<long[]> instance = newInstance();
        PrimitiveIterator.OfLong result = instance.keyIterator();
        for (int i = 0; i < 10; i++) {
            assertTrue(result.hasNext());
            assertEquals(i, result.nextLong());
        }
    }

    /**
     * Test of valueIterator method, of class LongLongSpinalList.
     */
    @Test
    public void testValueIterator() {
        LongLongSpinalList<long[]> instance = newInstance();
        PrimitiveIterator.OfLong result = instance.valueIterator();
        for (int i = 0; i < 10; i++) {
            assertTrue(result.hasNext());
            assertEquals(i, -result.nextLong());
        }

    }

    /**
     * Test of size method, of class LongLongSpinalList.
     */
    @Test
    public void testSize() {
        LongLongSpinalList<long[]> instance = newInstance();
        long expResult = 10L;
        long result = instance.size();
        assertEquals(expResult, result);
    }

    @Test
    public void testToAndFromDisk() throws IOException {
        File tmp = new File(anotherTempDir, "ll");
        boolean createNewFile = tmp.createNewFile();
        assertTrue(createNewFile);
        compareToDisk(tmp, newInstance(10));
        compareToDisk(tmp, newInstance(1_000_000));
    }

    private void compareToDisk(File tmp, LongLongSpinalList<long[]> instance) throws IOException {
        try ( BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tmp)); 
                DataOutputStream dos = new DataOutputStream(bos)) {
            instance.toStream(dos);
        }

        var fd = new LongLongSpinalList<>(name, gk, gv, comp);
        try ( RandomAccessFile raf = new RandomAccessFile(tmp, "r")) {
            fd.fromStream(raf);
            Iterator<long[]> iter = fd.iterator();
            Iterator<long[]> origIter = instance.iterator();
            while (iter.hasNext()) {
                assertTrue(origIter.hasNext());
                long[] next = iter.next();
                long[] origNext = origIter.next();
                assertArrayEquals(next, origNext);
            }
            assertFalse(origIter.hasNext());
        }
    }
}
