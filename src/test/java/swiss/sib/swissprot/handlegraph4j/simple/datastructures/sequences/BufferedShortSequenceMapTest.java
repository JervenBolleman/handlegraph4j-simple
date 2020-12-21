/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures.sequences;

import io.github.vgteam.handlegraph4j.sequences.LongSequence;
import io.github.vgteam.handlegraph4j.sequences.Sequence;
import io.github.vgteam.handlegraph4j.sequences.SequenceType;
import io.github.vgteam.handlegraph4j.sequences.ShortKnownSequence;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.io.TempDir;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class BufferedShortSequenceMapTest {

    @TempDir
    File temp;

    public BufferedShortSequenceMapTest() {
    }

    /**
     * Test of writeToDisk method, of class BufferedLongSequenceMap.
     *
     * @Throws IOException
     */
    @Test
    public void testWriteToDisk() throws IOException {
        ShortSequenceMap nodesWithSequences = new ShortSequenceMap();
        String[] nucleotides = new String[]{"a", "c", "t", "g"};
        for (int i = 0; i < 2067; i++) {
            addNucleotide(nucleotides[i % nucleotides.length], nodesWithSequences, i);
        }
        for (int i = 1; i < 32; i++) {
            byte[] s = new byte[i];
            Arrays.fill(s, (byte) 'n');
            Sequence seq = SequenceType.fromByteArray(s);
            nodesWithSequences.add(nodesWithSequences.size()+1, seq);
        }
        testEquivalence(nodesWithSequences);

    }

    private void addNucleotide(String nucleotide, ShortSequenceMap nodesWithSequences, int id) {
        byte[] bytes = nucleotide.getBytes(StandardCharsets.US_ASCII);
        nodesWithSequences.add(id, new ShortKnownSequence(bytes));
    }

    private void testEquivalence(ShortSequenceMap nodesWithLongSequences) throws IllegalStateException, IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try ( DataOutputStream raf = new DataOutputStream(byteArrayOutputStream)) {
            BufferedShortSequenceMap.writeToDisk(nodesWithLongSequences, raf);
        }
        File file = new File(temp, "tt");
        try ( OutputStream os = new FileOutputStream(file)) {
            os.write(byteArrayOutputStream.toByteArray());

        }
        try ( RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            BufferedShortSequenceMap blsm = new BufferedShortSequenceMap(raf);
            assertEquals(nodesWithLongSequences.size(), blsm.size());
            var readNodeSequences = blsm.nodeSequences();
            while (readNodeSequences.hasNext()) {
                var rdNext = readNodeSequences.next();
                long nodeId = rdNext.node().id();
                Sequence writenSeq = nodesWithLongSequences.getSequence(nodeId);
                assertEquals(writenSeq, rdNext.sequence());
            }
        }
    }

    @Test
    public void testBigWriteToDisk() throws IOException {
        ShortSequenceMap nodesWithLongSequences = new ShortSequenceMap();
        byte[] bytes = "a".getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i < 1047; i++) {
            nodesWithLongSequences.add(i, new LongSequence(bytes));
        }
        testEquivalence(nodesWithLongSequences);
    }
}
