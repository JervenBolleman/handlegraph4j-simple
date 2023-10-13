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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import swiss.sib.swissprot.handlegraph4j.simple.SimpleEdgeHandle;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;

/**
 *
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
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
		try (Stream<SimpleEdgeHandle> stream = instance.stream()) {
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
		try (Stream<SimpleEdgeHandle> stream = instance.stream()) {
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
		long[] expected = new long[] { 1, -1, 2, -2, 3, -3 };
		testTrimAndSortByArray(new long[] { 1, -1, 2, -2, 3, -4, 2, -3 }, new long[] { 1, -1, 2, -3, 2, -2, 3, -4 });
		testTrimAndSortByArray(new long[] { 1, -1, 2, -2, 3, -3 }, expected);
		testTrimAndSortByArray(new long[] { 1, -1, 3, -3, 2, -2 }, expected);
	}

	private void testTrimAndSortByArray(long[] field, long[] expected) {
		SimpleEdgeList instance = edgeListFromLongArray(field);
		instance.trimAndSort();
		var iterator = instance.iterator();
		for (int i = 0; i < field.length;) {
			assertTrue(iterator.hasNext());
			SimpleEdgeHandle eh = new SimpleEdgeHandle(expected[i++], expected[i++]);
			SimpleEdgeHandle next = iterator.next();
			assertEquals(eh, next);
		}
		assertFalse(iterator.hasNext());

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
		try (Stream<SimpleEdgeHandle> stream = instance.stream()) {
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

	@Test
	public void testIteratorGoingLeftWithEarlyTermination() {
		SimpleEdgeList instance = new SimpleEdgeList();
		int length = LongLongSpinalList.CHUNK_SIZE * 3;
		for (int i = 0; i < length;) {
			SimpleEdgeHandle eh = new SimpleEdgeHandle(++i, i);
			instance.add(eh);
		}
		instance.trimAndSort();
		assertEquals(LongLongSpinalList.CHUNK_SIZE * 3, instance.size());
		try (var iterator = instance.iterateToLeft(new SimpleNodeHandle(3))) {
			assertTrue(iterator.hasNext(), " at " + 3);
		}
		for (int i = 1; i < length; i += 1) {
			try (var iterator = instance.iterateToLeft(new SimpleNodeHandle(i))) {
				assertTrue(iterator.hasNext(), " at " + i);
				SimpleEdgeHandle eh = new SimpleEdgeHandle(i, i);
				assertEquals(eh, iterator.next(), " at " + i);
				assertFalse(iterator.hasNext(), " at " + i);
			}
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
		try (var iterator = instance.iterateToRight(new SimpleNodeHandle(2))) {
			assertTrue(iterator.hasNext());
			SimpleEdgeHandle eh = new SimpleEdgeHandle(1, 2);
			assertEquals(eh, iterator.next());
			assertFalse(iterator.hasNext());
		}
	}
}
