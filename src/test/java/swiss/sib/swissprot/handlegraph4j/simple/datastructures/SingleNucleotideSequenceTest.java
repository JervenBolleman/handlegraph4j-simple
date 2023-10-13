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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.github.jervenbolleman.handlegraph4j.sequences.Sequence;
import io.github.jervenbolleman.handlegraph4j.sequences.SequenceType;
import io.github.jervenbolleman.handlegraph4j.sequences.ShortAmbiguousSequence;
import io.github.jervenbolleman.handlegraph4j.sequences.ShortKnownSequence;

/**
 *
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 */
public class SingleNucleotideSequenceTest {

	public SingleNucleotideSequenceTest() {
	}

	/**
	 * Test of byteAt method, of class SingleNucleotideSequence.
	 */
	@Test
	public void testByteAt() {

		SingleNucleotideSequence instance = new SingleNucleotideSequence((byte) 'a');
		byte result = instance.byteAt(0);
		assertEquals('a', (char) result);
	}

	/**
	 * Test of length method, of class SingleNucleotideSequence.
	 */
	@Test
	public void testLength() {
		SingleNucleotideSequence instance = new SingleNucleotideSequence((byte) 'a');
		int expResult = 1;
		int result = instance.length();
		assertEquals(expResult, result);
	}

	/**
	 * Test of getType method, of class SingleNucleotideSequence.
	 */
	@Test
	public void testGetType() {

		SingleNucleotideSequence instance = new SingleNucleotideSequence((byte) 'a');
		SequenceType expResult = SequenceType.OTHER;
		SequenceType result = instance.getType();
		assertEquals(expResult, result);
	}

	/**
	 * Test of reverseComplement method, of class SingleNucleotideSequence.
	 */
	@Test
	public void testReverseComplement() {
		SingleNucleotideSequence instance = new SingleNucleotideSequence((byte) 'a');
		Sequence expResult = new SingleNucleotideSequence((byte) 't');
		Sequence result = instance.reverseComplement();
		assertEquals(expResult, result);
	}

	/**
	 * Test of hashCode method, of class SingleNucleotideSequence.
	 */
	@Test
	public void testHashCode() {
		for (byte b : new byte[] { 'a', 'c', 't', 'g' }) {
			Sequence instance = new SingleNucleotideSequence(b);
			Sequence obj = new ShortKnownSequence(new byte[] { b });
			assertEquals(obj.hashCode(), instance.hashCode());

		}

		for (Character c : Sequence.KNOWN_IUPAC_CODES) {
			byte b = (byte) c.charValue();
			Sequence instance = new SingleNucleotideSequence(b);
			Sequence obj = new ShortAmbiguousSequence(new byte[] { b });
			assertEquals(obj.hashCode(), instance.hashCode(), "at " + (char) b);
		}
	}

	/**
	 * Test of equals method, of class SingleNucleotideSequence.
	 */
	@Test
	public void testEquals() {
		for (byte b : new byte[] { 'a', 'c', 't', 'g' }) {
			Sequence instance = new SingleNucleotideSequence(b);
			Sequence obj = new ShortKnownSequence(new byte[] { b });
			assertTrue(instance.equals(obj), "at " + (char) b);
			assertTrue(obj.equals(instance), "at " + (char) b);
		}

		for (Character c : Sequence.KNOWN_IUPAC_CODES) {
			byte b = (byte) c.charValue();
			Sequence instance = new SingleNucleotideSequence(b);
			Sequence obj = new ShortAmbiguousSequence(new byte[] { b });
			assertTrue(instance.equals(obj));
			assertTrue(obj.equals(instance));
		}
	}

	/**
	 * Test of toString method, of class SingleNucleotideSequence.
	 */
	@Test
	public void testToString() {
		System.out.println("toString");
		SingleNucleotideSequence instance = new SingleNucleotideSequence((byte) 'a');
		String expResult = "a";
		String result = instance.toString();
		assertEquals(expResult, result);
	}

}
