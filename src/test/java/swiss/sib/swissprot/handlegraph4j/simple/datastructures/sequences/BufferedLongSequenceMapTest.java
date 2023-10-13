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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.jervenbolleman.handlegraph4j.sequences.LongSequence;

/**
 *
 @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
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
