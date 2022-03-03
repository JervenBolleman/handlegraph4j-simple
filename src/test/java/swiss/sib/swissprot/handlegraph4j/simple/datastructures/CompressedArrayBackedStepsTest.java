package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;

public class CompressedArrayBackedStepsTest {

	@Test
	public void basicTest() {
		long[] values = LongStream.range(0, 90_000).toArray();
		CompressedArrayBackedSteps toTest = new CompressedArrayBackedSteps(values);
		asserts(values, toTest);
	}

	@Test
	public void negativeTest() {
		long[] values = LongStream.range(-90_000, 0).toArray();
		CompressedArrayBackedSteps toTest = new CompressedArrayBackedSteps(values);
		asserts(values, toTest);
	}

	private void asserts(long[] values, CompressedArrayBackedSteps toTest) {
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

	@Test
	public void negativeAndPositiveTest() {
		long[] values = LongStream.range(-90_000, 0).toArray();
		for (int i = 0; i < values.length; i += 2) {
			values[i] = -values[i];
		}
		CompressedArrayBackedSteps toTest = new CompressedArrayBackedSteps(values);
		asserts(values, toTest);
	}
}
