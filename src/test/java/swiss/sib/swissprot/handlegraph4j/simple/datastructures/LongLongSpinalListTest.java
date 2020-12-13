/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class LongLongSpinalListTest {

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
        LongLongSpinalList.Reconstructor<long[]> name = (k, v) -> new long[]{k, v};
        LongLongSpinalList.ToLong<long[]> gk = a -> a[0];
        LongLongSpinalList.ToLong<long[]> gv = a -> a[1];
        Comparator<long[]> comp = (a, b) -> Long.compare(a[0], b[0]);
        LongLongSpinalList<long[]> instance = new LongLongSpinalList(name, gk, gv, comp);
        for (int i = 0; i < 10; i++) {
            instance.add(new long[]{i, -i});
        }
        instance.trimAndSort();
        return instance;
    }

    /**
     * Test of iterateToLeft method, of class LongLongSpinalList.
     */
    @Test
    public void testIterateToLeft() {
        LongLongSpinalList instance = newInstance();
        for (int i = 0; i < 10; i++) {
            try ( AutoClosedIterator<long[]> result = instance.iterateToLeft(i)) {
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
        System.out.println("isEmpty");
        LongLongSpinalList instance = newInstance();
        boolean expResult = false;
        boolean result = instance.isEmpty();
        assertEquals(expResult, result);
    }

    /**
     * Test of keyIterator method, of class LongLongSpinalList.
     */
    @Test
    public void testKeyIterator() {
        LongLongSpinalList instance = newInstance();
        PrimitiveIterator.OfLong result = instance.keyIterator();
        for (int i=0;i<10;i++){
            assertTrue(result.hasNext());
            assertEquals(i, result.nextLong());    
        }
    }

    /**
     * Test of valueIterator method, of class LongLongSpinalList.
     */
    @Test
    public void testValueIterator() {
        LongLongSpinalList instance = newInstance();
        PrimitiveIterator.OfLong result = instance.valueIterator();
        for (int i=0;i<10;i++){
            assertTrue(result.hasNext());
            assertEquals(i, -result.nextLong());    
        }

    }

    /**
     * Test of size method, of class LongLongSpinalList.
     */
    @Test
    public void testSize() {
        System.out.println("size");
        LongLongSpinalList instance = newInstance();
        long expResult = 10L;
        long result = instance.size();
        assertEquals(expResult, result);
    }
}
