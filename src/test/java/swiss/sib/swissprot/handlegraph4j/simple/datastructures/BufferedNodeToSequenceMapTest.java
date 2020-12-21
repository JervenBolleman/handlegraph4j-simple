/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import io.github.vgteam.handlegraph4j.sequences.Sequence;
import static io.github.vgteam.handlegraph4j.sequences.SequenceType.fromString;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.io.TempDir;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class BufferedNodeToSequenceMapTest {

    @TempDir
    File temp;

    public BufferedNodeToSequenceMapTest() {
    }

    /**
     * Test of nodes method, of class BufferedNodeToSequenceMap.
     *
     * @throws java.io.IOException
     */
    @Test
    public void testRandomAccess() throws IOException {
        InMemoryNodeToSequenceMap orig = new InMemoryNodeToSequenceMap();

        Sequence seq = fromString("CGGCAGAGCTCCCTCCTCAGCACACGG");
        int id = 142537850;
        add(orig, id, seq);
        orig.trim();
        assertEquals(seq, orig.getSequence(new SimpleNodeHandle(id)));
        File file = writeToDisk(orig);
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {

            BufferedNodeToSequenceMap instance = new BufferedNodeToSequenceMap(raf);

            assertEquals(seq, instance.getSequence(new SimpleNodeHandle(id)));
        }
    }

    @Test
    public void testRandomAccess2() throws IOException {
        InMemoryNodeToSequenceMap orig = new InMemoryNodeToSequenceMap();

        Sequence seq = fromString("CGGCAGAGCTCCCTCCTCAGCACACGG");
        Sequence seq2 = fromString("CGGCAGAGCTCCCTCCTCAGCACACGC");
        Sequence seq3 = fromString("CGGCAGAGCTCCCTCCTCAGCACACGT");
        Sequence seq4 = fromString("CGGCAGAGCTCCCTCCTCAGCACACGTCGGCAGAGCTCCCTCCTCAGCACACGTCGGCAGAGCTCCCTCCTCAGCACACGT");
        Sequence seq5 = fromString("CGGCAGAGCTCCCTCCTCAGCACACGTCGGCAGAGCTCCCTCCTCAGCACACGTCGGCAGAGCTCCCTCCTCAGCACACGTA");
        int id = 142537850;
        int idTwo = 142537851;
        int idThree = 1422337851;
        int idFour = 1422337891;
        int idFive = 1422337892;
        add(orig, id, seq);
        add(orig, idTwo, seq2);
        add(orig, idThree, seq3);
        add(orig, idFour, seq4);
        add(orig, idFive, seq5);
        orig.trim();
        Sequence origSeq4 = orig.getSequence(new SimpleNodeHandle(idFour));

        assertEquals(seq4, origSeq4);

        assertEquals(seq, orig.getSequence(new SimpleNodeHandle(id)));
        assertEquals(seq2, orig.getSequence(new SimpleNodeHandle(idTwo)));
        assertEquals(seq3, orig.getSequence(new SimpleNodeHandle(idThree)));

        assertEquals(seq5, orig.getSequence(new SimpleNodeHandle(idFive)));
        File file = writeToDisk(orig);
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {

            BufferedNodeToSequenceMap instance = new BufferedNodeToSequenceMap(raf);

            assertEquals(seq, instance.getSequence(new SimpleNodeHandle(id)));
            assertEquals(seq2, instance.getSequence(new SimpleNodeHandle(idTwo)));
            assertEquals(seq3, instance.getSequence(new SimpleNodeHandle(idThree)));
            assertEquals(seq4, instance.getSequence(new SimpleNodeHandle(idFour)));
            assertEquals(seq5, instance.getSequence(new SimpleNodeHandle(idFive)));
        }
    }

    private File writeToDisk(InMemoryNodeToSequenceMap orig) throws IOException {
        File file = new File(temp, "tt");
        try (OutputStream os = new FileOutputStream(file); DataOutputStream raf = new DataOutputStream(os)) {
            orig.writeToDisk(raf);
        }
        return file;
    }

    private void add(InMemoryNodeToSequenceMap orig, int i, Sequence sequence) {
        orig.add(i, sequence);
    }
}
