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
     * @Throws java.io.IOException
     */
    @Test
    public void testWriteToDisk() throws IOException {
        LongSequenceMap nodesWithLongSequences = new LongSequenceMap();
        byte[] bytes = "actgactgactgactgactgactgactgactg".getBytes(StandardCharsets.US_ASCII);
        nodesWithLongSequences.add(0, new LongSequence(bytes));
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
}
