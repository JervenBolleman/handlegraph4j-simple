/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures.sequences;

import io.github.vgteam.handlegraph4j.sequences.LongSequence;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.io.TempDir;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class BufferedLongSequenceMapTest {

    @TempDir
    File temp;

    public BufferedLongSequenceMapTest() {
    }

    /**
     * Test of writeToDisk method, of class BufferedLongSequenceMap.
     *
     * @Throws IOException
     */
    @Test
    public void testWriteToDisk() throws IOException {
        LongSequenceMap nodesWithLongSequences = new LongSequenceMap();
        addSequence("actgactgactgactgactgactgactgactg", nodesWithLongSequences, 0);
        addSequence("CGGCAGAGCTCCCTCCTCAGCACACGG", nodesWithLongSequences, 1);
        addSequence("CGGCAGAGCTCCCTCCTCAGCACACGGA", nodesWithLongSequences, 2);
        addSequence("CGGCAGAGCTCCCTCCTCAGCACACGGC", nodesWithLongSequences, 3);
        addSequence("CGGCAGAGCTCCCTCCTCAGCACACGGT", nodesWithLongSequences, 4);
        addSequence("CGGCAGAGCTCCCTCCTCAGCACACGGG", nodesWithLongSequences, 5);
        testReadinback(nodesWithLongSequences);

    }

    private void testReadinback(LongSequenceMap nodesWithLongSequences) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try ( DataOutputStream raf = new DataOutputStream(byteArrayOutputStream)) {
            BufferedLongSequenceMap.writeToDisk(nodesWithLongSequences, raf);
        }
        File file = new File(temp, "tt");
        try ( OutputStream os = new FileOutputStream(file)) {
            os.write(byteArrayOutputStream.toByteArray());

        }
        try ( RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            BufferedLongSequenceMap blsm = new BufferedLongSequenceMap(raf);
            assertEquals(nodesWithLongSequences.size(), blsm.size());
            var writtenNodeSequence = nodesWithLongSequences.nodeSequences();
            var readNodeSequences = blsm.nodeSequences();
            while (readNodeSequences.hasNext()) {
                assertTrue(writtenNodeSequence.hasNext());
                var wnNext = writtenNodeSequence.next();
                var rdNext = readNodeSequences.next();
                assertEquals(wnNext.node(), rdNext.node());
                assertEquals(wnNext.sequence(), rdNext.sequence());
            }
            assertFalse(writtenNodeSequence.hasNext());
        }
    }

    private void addSequence(String sequence, LongSequenceMap nodesWithLongSequences, int id) {
        byte[] bytes = sequence.getBytes(StandardCharsets.US_ASCII);
        nodesWithLongSequences.add(id, new LongSequence(bytes));
    }

    @Test
    public void testBigWriteToDisk() throws IOException {
        LongSequenceMap nodesWithLongSequences = new LongSequenceMap();
        byte[] bytes = "actgactgactgactgactgactgactgactg".getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i < 1047; i++) {
            nodesWithLongSequences.add(i, new LongSequence(bytes));
        }
        testReadinback(nodesWithLongSequences);

    }
}
