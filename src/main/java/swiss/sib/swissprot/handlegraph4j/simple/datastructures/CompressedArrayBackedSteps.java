/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.from;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PrimitiveIterator.OfLong;
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

	private interface Segment {
		long min();

		int length();

		long get(int i);

		LongStream stream();
	}

	private class LongSegment implements Segment {
		private final long[] values;

		public LongSegment(long[] values) {
			super();
			this.values = values;
		}

		@Override
		public long min() {
			return Arrays.stream(values).min().getAsLong();
		}

		@Override
		public int length() {
			return values.length;
		}

		@Override
		public long get(int i) {
			return values[i];
		}

		public LongStream stream() {
			return Arrays.stream(values);
		}
	}

	private class CompressedSegment implements Segment {
		public CompressedSegment(long[] values, long min, int length) {
			super();

			this.min = Arrays.stream(values).min().getAsLong();
			int[] intValues = new int[values.length];
			for (int i = 0; i < values.length; i++) {
				intValues[i] = (int) (values[i] - this.min);
			}
			this.values = iic.compress(intValues);
			this.length = length;
		}

		private final int[] values;
		private final long min;
		private final int length;

		@Override
		public long min() {
			return min;
		}

		@Override
		public int length() {
			return length;
		}

		@Override
		public long get(int i) {
			return iic.uncompress(values)[i] + this.min;
		}

		public LongStream stream() {
			int[] uncompress = iic.uncompress(values);
			return Arrays.stream(uncompress).mapToLong(i -> i + min);
		}
	}

	public CompressedArrayBackedSteps(long[] values) {
		int i = 0;
		while (i < values.length - SEGMENT_SIZE) {
			long min = values[i];
			long max = values[i];

			for (int j = i + 1; j < i + SEGMENT_SIZE; j++) {
				if (values[j] < min) {
					min = values[j];
				}
				if (values[j] > max) {
					max = values[j];
				}
			}
			long[] section = Arrays.copyOfRange(values, i, i + SEGMENT_SIZE);
			if (min + max < Integer.MAX_VALUE) {
				segments.add(new CompressedSegment(section, min, SEGMENT_SIZE));
			} else {
				segments.add(new LongSegment(section));
			}
			i += SEGMENT_SIZE;
		}
		if (i != values.length) {
			long[] section = Arrays.copyOfRange(values, i, values.length);
			segments.add(new LongSegment(section));
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
