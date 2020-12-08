/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import io.github.vgteam.handlegraph4j.sequences.Sequence;
import io.github.vgteam.handlegraph4j.sequences.SequenceType;
import io.github.vgteam.handlegraph4j.sequences.ShortAmbiguousSequence;
import io.github.vgteam.handlegraph4j.sequences.ShortKnownSequence;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
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
         for (byte b : new byte[]{'a', 'c', 't', 'g'}) {
            Sequence instance = new SingleNucleotideSequence(b);
            Sequence obj = new ShortKnownSequence(new byte[]{b});
            assertEquals(obj.hashCode(), instance.hashCode());
            
        }

        for (Character c : Sequence.KNOWN_IUPAC_CODES) {
            byte b = (byte) c.charValue();
            Sequence instance = new SingleNucleotideSequence(b);
            Sequence obj = new ShortAmbiguousSequence(new byte[]{b});
            assertEquals(obj.hashCode(), instance.hashCode(), "at " + (char) b);
        }
    }

    /**
     * Test of equals method, of class SingleNucleotideSequence.
     */
    @Test
    public void testEquals() {
        for (byte b : new byte[]{'a', 'c', 't', 'g'}) {
            Sequence instance = new SingleNucleotideSequence(b);
            Sequence obj = new ShortKnownSequence(new byte[]{b});
            assertTrue(instance.equals(obj), "at " + (char) b);
            assertTrue(obj.equals(instance), "at " + (char) b);
        }

        for (Character c : Sequence.KNOWN_IUPAC_CODES) {
            byte b = (byte) c.charValue();
            Sequence instance = new SingleNucleotideSequence(b);
            Sequence obj = new ShortAmbiguousSequence(new byte[]{b});
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
