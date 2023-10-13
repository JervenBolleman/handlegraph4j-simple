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
package swiss.sib.swissprot.handlegraph4j.simple.datastructures.sequences;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.junit.jupiter.api.io.TempDir;

import io.github.jervenbolleman.handlegraph4j.sequences.LongSequence;
import io.github.jervenbolleman.handlegraph4j.sequences.Sequence;
import io.github.jervenbolleman.handlegraph4j.sequences.SequenceType;
import io.github.jervenbolleman.handlegraph4j.sequences.ShortKnownSequence;

/**
 *
 @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
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
