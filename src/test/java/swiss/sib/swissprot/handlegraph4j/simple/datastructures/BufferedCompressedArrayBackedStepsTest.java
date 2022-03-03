package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;

public class BufferedCompressedArrayBackedStepsTest {
	@TempDir
	File temp;

	@Test
	public void basicTest() throws IOException {
		long[] values = LongStream.range(0, 90_000).toArray();
		CompressedArrayBackedSteps toTest = new CompressedArrayBackedSteps(values);
		asserts(values, toTest);
	}

	@Test
	public void negativeTest() throws IOException {
		long[] values = LongStream.range(-90_000, 0).toArray();
		CompressedArrayBackedSteps toTest = new CompressedArrayBackedSteps(values);
		asserts(values, toTest);
	}

	@Test
	public void negativeAndPositiveTest() throws IOException {
		long[] values = LongStream.range(-90_000, 0).toArray();
		for (int i = 0; i < values.length; i += 2) {
			values[i] = -values[i];
		}
		CompressedArrayBackedSteps toTest = new CompressedArrayBackedSteps(values);
		asserts(values, toTest);
	}

	private void asserts(long[] values, Steps toTest) throws IOException {
		File file = new File(temp, "steps");
		write(toTest, file);
		read(values, file);
	}

	private void read(long[] values, File file) throws IOException, FileNotFoundException {
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			Steps toTest = new BufferedCompressedArrayBackedSteps(raf);
			assertEquals(toTest.length(), values.length);
			for (int i = 0; i < values.length; i++) {
				assertEquals(toTest.nodeIdOfStep(i), values[i]);
			}

			try (AutoClosedIterator<SimpleNodeHandle> nodes = toTest.nodes()) {
				for (int i = 0; i < values.length; i++) {
					assertEquals(nodes.next().id(), values[i]);
				}
			}
		}
	}

	private void write(Steps toTest, File file) throws IOException, FileNotFoundException {
		try (FileOutputStream fos = new FileOutputStream(file);
				BufferedOutputStream bos = new BufferedOutputStream(fos);
				DataOutputStream dos = new DataOutputStream(bos)) {
			BufferedCompressedArrayBackedSteps.write(dos, toTest);
		}
	}

}
