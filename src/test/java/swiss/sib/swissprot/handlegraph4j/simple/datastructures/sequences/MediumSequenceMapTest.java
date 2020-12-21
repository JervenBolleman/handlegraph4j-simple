/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures.sequences;

import io.github.vgteam.handlegraph4j.sequences.Sequence;
import io.github.vgteam.handlegraph4j.sequences.SequenceType;
import io.github.vgteam.handlegraph4j.sequences.ShortAmbiguousSequence;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class MediumSequenceMapTest {

    public MediumSequenceMapTest() {
    }

    /**
     * Test of add method, of class MediumSequenceMap.
     */
    @Test
    public void testAdd_NodeSequence() {
        MediumSequenceMap instance = new MediumSequenceMap();
        for (int i = 1; i < ShortAmbiguousSequence.MAX_LENGTH; i++) {
            byte[] s = new byte[i];
            Arrays.fill(s, (byte) 'n');
            Sequence seq = SequenceType.fromByteArray(s);
            instance.add(i, seq);
        }
        instance.trim();
        for (int i = 1; i < ShortAmbiguousSequence.MAX_LENGTH; i++) {
            byte[] s = new byte[i];
            Arrays.fill(s, (byte) 'n');
            Sequence seq = SequenceType.fromByteArray(s);
            assertEquals(seq, instance.getSequence(i));
        }
    }
}
