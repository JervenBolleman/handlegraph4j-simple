/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

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
        instance.trimAndSort();
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
    public void testAdd_long_long() {
        SimpleEdgeHandle eh = new SimpleEdgeHandle(1, 2);
        SimpleEdgeList instance = new SimpleEdgeList();
        instance.add(eh);
        instance.trimAndSort();
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
    public void testTrimAndSort() {
        long[] expected = new long[]{1, -1, 2, -2, 3, -3};
        testTrimAndSortByArray(new long[]{1, -1, 2, -2, 3, -4, 2, -3}, new long[]{1, -1, 2, -3, 2, -2, 3, -4});
        testTrimAndSortByArray(new long[]{1, -1, 2, -2, 3, -3}, expected);
        testTrimAndSortByArray(new long[]{1, -1, 3, -3, 2, -2}, expected);
    }

    private void testTrimAndSortByArray(long[] field, long[] expected) {
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
//        assertFalse(instance.isNotSorted());
    }

    private SimpleEdgeList edgeListFromLongArray(long[] field) {
        SimpleEdgeList instance = new SimpleEdgeList();
        for (int i = 0; i < field.length;) {
            instance.add(new SimpleEdgeHandle(field[i++], field[i++]));
        }
        return instance;
    }

    /**
     * Test of iterator method, of class SimpleEdgeList.
     */
    @Test
    public void testSmallIterator() {
        SimpleEdgeList instance = new SimpleEdgeList();

        int length = 10;
        testIterator(length, instance);
    }

    private void testIterator(int length, SimpleEdgeList instance) {
        for (int i = 0; i < length;) {
            SimpleEdgeHandle eh = new SimpleEdgeHandle(++i, ++i);
            instance.add(eh);
        }
        instance.trimAndSort();
        try ( Stream<SimpleEdgeHandle> stream = instance.stream()) {
            var iterator = stream.iterator();
            for (int i = 0; i < length;) {
                assertTrue(iterator.hasNext(), " at " + i);
                SimpleEdgeHandle eh = new SimpleEdgeHandle(++i, ++i);
                assertEquals(eh, iterator.next(), " at " + i);
            }
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    public void testBigValueIterator() {
        SimpleEdgeList instance = new SimpleEdgeList();

        int length = LongLongSpinalList.CHUNK_SIZE * 3;
        testIterator(length, instance);
    }

    /**
     * Test of iterator method, of class SimpleEdgeList.
     */
    @Test
    public void testIteratorGoingLeftWithEarlyTermination()  {
        SimpleEdgeList instance = new SimpleEdgeList();
        int length = 10;
        for (int i = 0; i < length;) {
            SimpleEdgeHandle eh = new SimpleEdgeHandle(++i, ++i);
            instance.add(eh);
        }
        instance.trimAndSort();
        try ( var stream = instance.streamToLeft(new SimpleNodeHandle(1))) {
            var iterator = stream.iterator();
            assertTrue(iterator.hasNext());
            SimpleEdgeHandle eh = new SimpleEdgeHandle(1, 2);
            assertEquals(eh, iterator.next());
            assertFalse(iterator.hasNext());
        }

        try ( var stream = instance.streamToLeft(new SimpleNodeHandle(3))) {
            var iterator = stream.iterator();
            assertTrue(iterator.hasNext());
            SimpleEdgeHandle eh = new SimpleEdgeHandle(3, 4);
            assertEquals(eh, iterator.next());
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    public void testIteratorGoingRightWithEarlyTermination() {
        SimpleEdgeList instance = new SimpleEdgeList();
        int length = 10;
        for (int i = 0; i < length;) {
            SimpleEdgeHandle eh = new SimpleEdgeHandle(++i, ++i);
            instance.add(eh);
        }
        instance.trimAndSort();
        try ( var stream = instance.streamToRight(new SimpleNodeHandle(2))) {
            var iterator = stream.iterator();
            assertTrue(iterator.hasNext());
            SimpleEdgeHandle eh = new SimpleEdgeHandle(1, 2);
            assertEquals(eh, iterator.next());
            assertFalse(iterator.hasNext());
        }
    }
}
