/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.from;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.map;

import java.util.ArrayList;
import java.util.List;
import java.util.PrimitiveIterator.OfLong;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import me.lemire.integercompression.IntCompressor;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class CompressedArrayBackedSteps implements Steps {

	private static final int SEGMENT_SIZE = 8192;
	private static final IntCompressor iic = new IntCompressor();
	private final List<Segment> segments = new ArrayList<>();
	private final int length;

	private class Segment {
		public Segment(long[] values, int start, int end) {
			super();

			this.length = end - start;
			int[] rawLowBits = new int[this.length];
			int[] rawHighBits = new int[this.length];
			for (int i = 0; i + start < end; i++) {
				rawHighBits[i] = (int) (values[i + start]);
				rawLowBits[i] = (int) (values[i + start] >>> Integer.SIZE);
			}
			this.lowbits = iic.compress(rawLowBits);
			this.highbits = iic.compress(rawHighBits);
		}

		private final int[] highbits;
		private final int[] lowbits;
		private final int length;

		public int length() {
			return length;
		}

		public long get(int i) {
			int rawLow = iic.uncompress(lowbits)[i];
			long low = ((long) rawLow) << Integer.SIZE;
			return low | (long) iic.uncompress(highbits)[i];
		}

		public LongStream stream() {
			int[] rawLowBits = iic.uncompress(lowbits);
			int[] rawHighBits = iic.uncompress(highbits);
			return IntStream.range(0, length)
					.mapToLong(i -> (((long) rawLowBits[i]) << Integer.SIZE) | (long) rawHighBits[i]);
		}
	}

	public CompressedArrayBackedSteps(long[] values) {
		int i = 0;
		while (i < values.length - SEGMENT_SIZE) {

			segments.add(new Segment(values, i, i + SEGMENT_SIZE));

			i += SEGMENT_SIZE;
		}
		if (i != values.length) {
			segments.add(new Segment(values, i, values.length));
		}
		length = values.length;
	}

	@Override
	public SimpleNodeHandle firstNode() {

		return new SimpleNodeHandle(segments.get(0).get(0));
	}

	@Override
	public SimpleNodeHandle lastNode() {
		Segment lastSegment = segments.get(segments.size() - 1);
		return new SimpleNodeHandle(lastSegment.get(lastSegment.length() - 1));
	}

	@Override
	public AutoClosedIterator<SimpleNodeHandle> nodes() {
		OfLong n = segments.stream().flatMapToLong(Segment::stream).iterator();
		return map(from(n), SimpleNodeHandle::new);
	}

	@Override
	public long length() {
		return this.length;
	}

	@Override
	public long nodeIdOfStep(long rank) {
		int segIndex = (int) rank / SEGMENT_SIZE;

		return segments.get(segIndex).get((int) (rank % SEGMENT_SIZE));
	}

}
