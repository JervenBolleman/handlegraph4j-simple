/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import swiss.sib.swissprot.handlegraph4j.simple.datastructures.SimpleEdgeList;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleEdgeHandle;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class SimpleEdgeListTest {

    public SimpleEdgeListTest() {
    }

    /**
     * Test of add method, of class SimpleEdgeList.
     */
    @Test
    public void testAdd_SimpleEdgeHandle() {
        SimpleEdgeHandle eh = new SimpleEdgeHandle(1, 2);
        SimpleEdgeList instance = new SimpleEdgeList();
        instance.add(eh);
        try ( Stream<SimpleEdgeHandle> stream = instance.stream()) {
            var iterator = stream.iterator();
            assertTrue(iterator.hasNext());
            assertEquals(eh, iterator.next());
            assertFalse(iterator.hasNext());
        }
    }

    /**
     * Test of add method, of class SimpleEdgeList.
     */
    @Test
    public void testAdd_long_long() throws Exception {
        SimpleEdgeHandle eh = new SimpleEdgeHandle(1, 2);
        long left = 1L;
        long right = 2L;
        SimpleEdgeList instance = new SimpleEdgeList();
        instance.add(left, right);
        try ( Stream<SimpleEdgeHandle> stream = instance.stream()) {
            var iterator = stream.iterator();
            assertTrue(iterator.hasNext());
            assertEquals(eh, iterator.next());
            assertFalse(iterator.hasNext());
        }
    }

    /**
     * Test of trimAndSort method, of class SimpleEdgeList.
     */
    @Test
    public void testTrimAndSort() throws Exception {
        long[] expected = new long[]{1, -1, 2, -2, 3, -3};
        testTrimAndSortByArray(new long[]{1, -1, 2, -2, 3, -4, 2, -3}, new long[]{1, -1, 2, -3, 2, -2, 3, -4});
        testTrimAndSortByArray(new long[]{1, -1, 2, -2, 3, -3}, expected);
        testTrimAndSortByArray(new long[]{1, -1, 3, -3, 2, -2}, expected);

    }

    private void testTrimAndSortByArray(long[] field, long[] expected) throws Exception {
        SimpleEdgeList instance = edgeListFromLongArray(field);
        instance.trimAndSort();
        try ( Stream<SimpleEdgeHandle> stream = instance.stream()) {
            var iterator = stream.iterator();
            for (int i = 0; i < field.length;) {
                assertTrue(iterator.hasNext());
                SimpleEdgeHandle eh = new SimpleEdgeHandle(expected[i++], expected[i++]);
                assertEquals(eh, iterator.next());
            }
            assertFalse(iterator.hasNext());
        }
        assertFalse(instance.isNotSorted());
    }

    private SimpleEdgeList edgeListFromLongArray(long[] field) {
        SimpleEdgeList instance = new SimpleEdgeList();
        for (int i = 0; i < field.length;) {
            instance.add(field[i++], field[i++]);
        }
        return instance;
    }

    /**
     * Test of iterator method, of class SimpleEdgeList.
     */
    @Test
    public void testIterator() {
        SimpleEdgeList instance = new SimpleEdgeList();
        int length = 10;
        for (int i = 0; i < length;) {
            SimpleEdgeHandle eh = new SimpleEdgeHandle(++i, ++i);
            instance.add(eh);
        }

        try ( Stream<SimpleEdgeHandle> stream = instance.stream()) {
            var iterator = stream.iterator();
            for (int i = 0; i < length;) {
                assertTrue(iterator.hasNext());
                SimpleEdgeHandle eh = new SimpleEdgeHandle(++i, ++i);
                assertEquals(eh, iterator.next());
            }
            assertFalse(iterator.hasNext());
        }
    }

    /**
     * Test of iterator method, of class SimpleEdgeList.
     */
    @Test
    public void testIteratorGoingLeftWithEarlyTermination() throws Exception {
        SimpleEdgeList instance = new SimpleEdgeList();
        int length = 10;
        for (int i = 0; i < length;) {
            SimpleEdgeHandle eh = new SimpleEdgeHandle(++i, ++i);
            instance.add(eh);
        }
        try ( Stream<SimpleEdgeHandle> stream = instance.streamToLeft(new SimpleNodeHandle(1))) {
            var iterator = stream.iterator();
            for (int i = 0; i < length;) {
                assertTrue(iterator.hasNext());
                SimpleEdgeHandle eh = new SimpleEdgeHandle(++i, ++i);
                assertEquals(eh, iterator.next());
            }
            assertFalse(iterator.hasNext());
        }

        try ( var stream = instance.streamToLeft(new SimpleNodeHandle(1))) {
            var iterator = stream.iterator();
            assertTrue(iterator.hasNext());
            SimpleEdgeHandle eh = new SimpleEdgeHandle(1, 2);
            assertEquals(eh, iterator.next());
            assertTrue(iterator.hasNext());
        }

        try ( var stream = instance.streamToLeft(new SimpleNodeHandle(3))) {
            var iterator = stream.iterator();
            assertTrue(iterator.hasNext());
            for (int i = 2; i < length;) {
                assertTrue(iterator.hasNext());
                SimpleEdgeHandle eh = new SimpleEdgeHandle(++i, ++i);
                assertEquals(eh, iterator.next());
            }
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    public void testIteratorGoingRightWithEarlyTermination() throws Exception {
        SimpleEdgeList instance = new SimpleEdgeList();
        int length = 10;
        for (int i = 0; i < length;) {
            SimpleEdgeHandle eh = new SimpleEdgeHandle(++i, ++i);
            instance.add(eh);
        }
        try ( var stream = instance.streamToRight(new SimpleNodeHandle(2))) {
            var iterator = stream.iterator();
            assertTrue(iterator.hasNext());
            SimpleEdgeHandle eh = new SimpleEdgeHandle(1, 2);
            assertEquals(eh, iterator.next());
            assertFalse(iterator.hasNext());
        }
    }
}
