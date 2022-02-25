/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures.sequences;

import io.github.vgteam.handlegraph4j.sequences.LongSequence;
import io.github.vgteam.handlegraph4j.sequences.Sequence;
import io.github.vgteam.handlegraph4j.sequences.SequenceType;
import io.github.vgteam.handlegraph4j.sequences.ShortAmbiguousSequence;
import static java.nio.charset.StandardCharsets.US_ASCII;

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

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class MediumSequenceMapTest {
	@TempDir
	File temp;

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

	@Test
	public void testAdd_NodeSequence2() {
		MediumSequenceMap instance = new MediumSequenceMap();

		Sequence seq = SequenceType.fromByteArray("CGGCAGAGCTCCCTCCTCAGCACACGG".getBytes(US_ASCII));
		instance.add(0, seq);

		instance.trim();
		assertEquals(seq, instance.getSequence(0));
	}

	@Test
	public void testWriteToDisk() throws IOException {
		MediumSequenceMap nodesWithLongSequences = new MediumSequenceMap();
		addSequence("actgactgactgactgactgactgactgactg", nodesWithLongSequences, 0);
		addSequence("CGGCAGAGCTCCCTCCTCAGCACACGG", nodesWithLongSequences, 1);
		addSequence("CGGCAGAGCTCCCTCCTCAGCACACGGA", nodesWithLongSequences, 2);
		addSequence("CGGCAGAGCTCCCTCCTCAGCACACGGC", nodesWithLongSequences, 3);
		addSequence("CGGCAGAGCTCCCTCCTCAGCACACGGT", nodesWithLongSequences, 4);
		addSequence("CGGCAGAGCTCCCTCCTCAGCACACGGG", nodesWithLongSequences, 5);
		testReadinback(nodesWithLongSequences);

	}

	private void testReadinback(MediumSequenceMap nodesWithLongSequences) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try (DataOutputStream raf = new DataOutputStream(byteArrayOutputStream)) {
			MediumSequenceMap.writeToDisk(nodesWithLongSequences, raf);
		}
		File file = new File(temp, "tt");
		try (OutputStream os = new FileOutputStream(file)) {
			os.write(byteArrayOutputStream.toByteArray());

		}
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			MediumSequenceMap blsm = MediumSequenceMap.open(raf);
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

	private void addSequence(String sequence, MediumSequenceMap nodesWithLongSequences, int id) {
		byte[] bytes = sequence.getBytes(StandardCharsets.US_ASCII);
		nodesWithLongSequences.add(id, new LongSequence(bytes));
	}
}
