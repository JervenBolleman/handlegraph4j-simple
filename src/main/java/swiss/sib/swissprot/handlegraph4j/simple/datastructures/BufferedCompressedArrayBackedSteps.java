/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.from;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.map;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
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
public class BufferedCompressedArrayBackedSteps implements Steps {

	private static final int SEGMENT_SIZE = 8192;
	private static final IntCompressor iic = new IntCompressor();
	private final List<Segment> segments = new ArrayList<>();
	private final int length;

	private static class Segment {
		public Segment(RandomAccessFile raf) throws IOException {
			super();
			this.lowBitsLength = raf.readInt();
			this.highBitsLength = raf.readInt();
			int endOfSegment = (lowBitsLength * Integer.BYTES) + (highBitsLength * Integer.BYTES);
			MappedByteBuffer map = raf.getChannel().map(MapMode.READ_ONLY, raf.getFilePointer(), endOfSegment);
			this.data = map.asIntBuffer();
			raf.seek(raf.getFilePointer() + endOfSegment);
		}

		public Segment(MappedByteBuffer map, int offset) throws IOException {
			super();
			this.lowBitsLength = map.getInt(offset);
			this.highBitsLength = map.getInt(offset + Integer.BYTES);
			int endOfSegment = (lowBitsLength * Integer.BYTES) + (highBitsLength * Integer.BYTES);
			int from = offset + (2 * Integer.BYTES);
			this.data = map.slice(from, endOfSegment).asIntBuffer();
			assert(this.data.limit() == this.lowBitsLength +this.highBitsLength);
//			map.position(map.position() + endOfSegment);
		}

		private final IntBuffer data;
		private final int lowBitsLength;
		private final int highBitsLength;

		public int length() {
			return data.limit();
		}

		public long get(int i) {

			int rawLow = uncompressLow()[i];
			long low = ((long) rawLow) << Integer.SIZE;
			long high = uncompressHigh()[i];
			return low | high;
		}

		private int[] uncompressHigh() {
			int[] highBits = new int[highBitsLength];
			data.get(lowBitsLength, highBits, 0, highBitsLength);
			return iic.uncompress(highBits);
		}

		private int[] uncompressLow() {
			int[] lowBits = new int[lowBitsLength];
			data.get(0, lowBits, 0, lowBitsLength);
			return iic.uncompress(lowBits);
		}

		public LongStream stream() {
			int[] rawLowBits = uncompressLow();
			int[] rawHighBits = uncompressHigh();
			return IntStream.range(0, rawLowBits.length)
					.mapToLong(i -> (((long) rawLowBits[i]) << Integer.SIZE) | (long) rawHighBits[i]);
		}
	}

	public BufferedCompressedArrayBackedSteps(RandomAccessFile raf) throws IOException {
		int length = raf.readInt();
		int i = 0;
		long start = raf.getFilePointer();
		int readSegments = 0;
		List<Integer> segmentStarts = new ArrayList<>();
		while (i < length - SEGMENT_SIZE) {
			segmentStarts.add((int) (raf.getFilePointer() - start));
			int lowBitsLength = raf.readInt();
			int highBitsLength = raf.readInt();
			raf.skipBytes((highBitsLength * Integer.BYTES) + (lowBitsLength * Integer.BYTES));
			readSegments++;
			if (raf.getFilePointer() - start > Integer.MAX_VALUE) {
				openSteps(raf, start, readSegments, segmentStarts);
				readSegments = 0;
				start = raf.getFilePointer();
				segmentStarts.clear();
			}
			i += SEGMENT_SIZE;
		}

		openSteps(raf, start, readSegments, segmentStarts);

		if (i != length) {
			segments.add(new Segment(raf));
		}
		this.length = length;
	}

	private void openSteps(RandomAccessFile raf, long start, int readSegments, List<Integer> segmentStarts)
			throws IOException {
		MappedByteBuffer map = raf.getChannel().map(MapMode.READ_ONLY, start, raf.getFilePointer() - start);
		for (int j = 0; j < readSegments; j++) {
			segments.add(new Segment(map, segmentStarts.get(j)));
		}
	}

	public static void write(DataOutputStream dos, Steps steps) throws IOException {
		int i = 0;
		int length = (int) steps.length();
		dos.writeInt(length);
		try (AutoClosedIterator<SimpleNodeHandle> nodes = steps.nodes()) {
			while (i < length - SEGMENT_SIZE) {
				long[] values = new long[SEGMENT_SIZE];
				writeSegment(dos, nodes, values);
				i += SEGMENT_SIZE;
			}
			if (i != length) {
				long[] values = new long[length - i];
				writeSegment(dos, nodes, values);
			}
		}
	}

	private static void writeSegment(DataOutputStream dos, AutoClosedIterator<SimpleNodeHandle> nodes, long[] values)
			throws IOException {
		for (int i = 0; i < values.length && nodes.hasNext(); i++) {
			values[i] = nodes.next().id();
		}
		int[] rawLowBits = new int[values.length];
		int[] rawHighBits = new int[values.length];
		for (int i = 0; i < values.length; i++) {
			rawHighBits[i] = (int) (values[i]);
			rawLowBits[i] = (int) (values[i] >>> Integer.SIZE);
		}
		int[] lowbits = iic.compress(rawLowBits);
		int[] highbits = iic.compress(rawHighBits);
		dos.writeInt(lowbits.length);
		dos.writeInt(highbits.length);
		writeInts(dos, lowbits);
		writeInts(dos, highbits);
	}

	private static void writeInts(DataOutputStream dos, int[] highbits) throws IOException {
		for (int i = 0; i < highbits.length; i++) {
			dos.writeInt(highbits[i]);
		}
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
