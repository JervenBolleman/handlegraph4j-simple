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
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
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

	private static final int MIN_MAP_BLOCK_SIZE = Integer.MAX_VALUE / 4;
	private static final int SEGMENT_SIZE = 8192;
	private static final IntCompressor iic = new IntCompressor();
	private final List<Segment> segments = new ArrayList<>();
	private final int length;

	private interface Segment {

		int length();

		long get(int i);

		LongStream stream();
	}

	private class CompressedSegment implements Segment {
		private final int compressedLength;
		private final MappedByteBuffer data;
		private final int offset;
		private final long min;
		private final int length;

		public CompressedSegment(MappedByteBuffer map, int offset) {
			super();
			this.offset = offset;
			this.compressedLength = map.getShort(offset);
			this.length = map.getShort(offset + Short.BYTES);
			this.min = map.getLong(offset + Short.BYTES + Short.BYTES);
			this.data = map;
		}

		static void skip(RandomAccessFile in) throws IOException {

//			long start = in.getFilePointer();
			short incompressed = in.readShort();
			in.skipBytes((incompressed * Integer.BYTES) + Short.BYTES + Long.BYTES);
//			long end = in.getFilePointer();
//			System.err.println("Skipped" + (start - end));
		}

		static void write(DataOutputStream dos, long[] values) throws IOException {
//			long start = dos.size();
			long min = Arrays.stream(values).min().getAsLong();
			int[] intValues = new int[values.length];
			for (int i = 0; i < values.length; i++) {
				intValues[i] = (int) (values[i] - min);
			}
			int[] compressedValues = iic.compress(intValues);
			dos.writeShort(compressedValues.length);
			dos.writeShort(values.length);
			dos.writeLong(min);
			writeInts(dos, compressedValues);
//			long end = dos.size();
//			System.err.println("Wrote" + (start - end));
		}

		@Override
		public int length() {
			return length;
		}

		@Override
		public long get(int x) {
			int[] uncompress = uncompress();
			return uncompress[x] + this.min;
		}

		private int[] uncompress() {
			int[] values = new int[compressedLength];
			for (int i = 0; i < compressedLength; i++) {
				values[i] = data.getInt(offset + (Short.BYTES + Short.BYTES + Long.BYTES) + (i * Integer.BYTES));
			}
			int[] uncompress = iic.uncompress(values);
			return uncompress;
		}

		public LongStream stream() {
			int[] uncompress = uncompress();
			return Arrays.stream(uncompress).mapToLong(i -> i + min);
		}

	}

	private static class LessCompressedSegment implements Segment {
		public LessCompressedSegment(RandomAccessFile raf) throws IOException {
			super();
			this.lowBitsLength = raf.readShort();
			this.highBitsLength = raf.readShort();
			int endOfSegment = (lowBitsLength * Integer.BYTES) + (highBitsLength * Integer.BYTES);
			MappedByteBuffer map = raf.getChannel().map(MapMode.READ_ONLY, raf.getFilePointer(), endOfSegment);
			this.offset = 0;

			this.data = map;
			raf.seek(raf.getFilePointer() + endOfSegment);
			this.length = uncompressLow().length;
		}

		public LessCompressedSegment(MappedByteBuffer map, int offset) throws IOException {
			super();
			this.offset = offset + (Short.BYTES * 2);
			this.lowBitsLength = map.getShort(offset);
			this.highBitsLength = map.getShort(offset + Short.BYTES);
			this.data = map;
//			assert(this.data.limit() == this.lowBitsLength +this.highBitsLength);
			this.length = uncompressLow().length;
		}

		static void skip(RandomAccessFile in) throws IOException {
//			long start = in.getFilePointer();
			int lowBitsLength = in.readShort();
			int highBitsLength = in.readShort();
			in.skipBytes((lowBitsLength + highBitsLength) * Short.BYTES);
//			long end = in.getFilePointer();
//			System.err.println("Skipped" + (start - end));
		}

		private final ByteBuffer data;
		private final int lowBitsLength;
		private final int highBitsLength;
		private final int offset;
		private final int length;

		public int length() {
			return length;
		}

		public long get(int i) {

			int rawLow = uncompressLow()[i];
			long low = ((long) rawLow) << Integer.SIZE;
			long high = uncompressHigh()[i];
			return low | high;
		}

		private int[] uncompressHigh() {
			int[] highBits = new int[highBitsLength];
			for (int i = 0; i < highBitsLength; i++) {
				highBits[i] = data.getInt(offset + (lowBitsLength * Integer.BYTES) + (i * Integer.BYTES));
			}
			return iic.uncompress(highBits);
		}

		private int[] uncompressLow() {
			int[] lowBits = new int[lowBitsLength];
			for (int i = 0; i < lowBitsLength; i++) {
				lowBits[i] = data.getInt(offset + (i * Integer.BYTES));
			}
			return iic.uncompress(lowBits);
		}

		public LongStream stream() {
			int[] rawLowBits = uncompressLow();
			int[] rawHighBits = uncompressHigh();
			return IntStream.range(0, rawLowBits.length)
					.mapToLong(i -> (((long) rawLowBits[i]) << Integer.SIZE) | (long) rawHighBits[i]);
		}

		static void write(DataOutputStream dos, long[] values) throws IOException {
//			long start = dos.size();
			int[] rawLowBits = new int[values.length];
			int[] rawHighBits = new int[values.length];
			for (int i = 0; i < values.length; i++) {
				rawHighBits[i] = (int) (values[i]);
				rawLowBits[i] = (int) (values[i] >>> Integer.SIZE);
			}
			int[] lowbits = iic.compress(rawLowBits);
			int[] highbits = iic.compress(rawHighBits);
			dos.writeShort(lowbits.length);
			dos.writeShort(highbits.length);
			writeInts(dos, lowbits);
			writeInts(dos, highbits);
//			long end = dos.size();
//			System.err.println("Wrote " + (start - end));
		}
	}

	public BufferedCompressedArrayBackedSteps(RandomAccessFile raf) throws IOException {
		this.length = raf.readInt();
		int i = 0;
		long start = raf.getFilePointer();
		int readSegments = 0;
		List<Integer> segmentStarts = new ArrayList<>();
		BitSet compressionState = new BitSet();
		while (i < this.length - SEGMENT_SIZE) {
			boolean compressed = raf.readBoolean();
			segmentStarts.add((int) (raf.getFilePointer() - start));
			compressionState.set(readSegments, compressed);
			if (compressed) {
				CompressedSegment.skip(raf);
			} else {
				LessCompressedSegment.skip(raf);
			}
			readSegments++;
			if (raf.getFilePointer() - start > MIN_MAP_BLOCK_SIZE) {
				openSteps(raf, start, readSegments, segmentStarts, compressionState);
				readSegments = 0;
				start = raf.getFilePointer();
				segmentStarts.clear();
				compressionState.clear();
			}
			i += SEGMENT_SIZE;
		}

		if (i != length) {
			boolean compressed = raf.readBoolean();
			segmentStarts.add((int) (raf.getFilePointer() - start));
			compressionState.set(readSegments, compressed);
			if (compressed) {
				CompressedSegment.skip(raf);
			} else {
				LessCompressedSegment.skip(raf);
			}
			readSegments++;
			
		}
		openSteps(raf, start, readSegments, segmentStarts, compressionState);
	}

	private void openSteps(RandomAccessFile raf, long start, int readSegments, List<Integer> segmentStarts,
			BitSet compressionState) throws IOException {
		MappedByteBuffer map = raf.getChannel().map(MapMode.READ_ONLY, start, raf.getFilePointer() - start);
		for (int j = 0; j < readSegments; j++) {
			if (compressionState.get(j)) {
				segments.add(new CompressedSegment(map, segmentStarts.get(j)));
			} else {
				segments.add(new LessCompressedSegment(map, segmentStarts.get(j)));
			}
		}
	}

	public static void write(DataOutputStream dos, Steps steps) throws IOException {
		int i = 0;
		int length = (int) steps.length();
		dos.writeInt(length);
		try (AutoClosedIterator<SimpleNodeHandle> nodes = steps.nodes()) {
			while (i < length - SEGMENT_SIZE) {
				long[] values = new long[SEGMENT_SIZE];
				fillValues(nodes, values);
				writeSegment(dos, values);
				i += SEGMENT_SIZE;
			}
			if (i != length) {
				long[] values = new long[length - i];
				fillValues(nodes, values);
				writeSegment(dos, values);
			}
		}
	}

	private static void writeSegment(DataOutputStream dos, long[] values)
			throws IOException {
		
		if (testIfHighlyCompressible(values)) {
			dos.writeBoolean(true);
			CompressedSegment.write(dos, values);
		} else {
			dos.writeBoolean(false);
			LessCompressedSegment.write(dos, values);
		}
	}

	private static void fillValues(AutoClosedIterator<SimpleNodeHandle> nodes, long[] values) {
		for (int i = 0; i < values.length && nodes.hasNext(); i++) {
			values[i] = nodes.next().id();
		}
	}

	private static boolean testIfHighlyCompressible(long[] values) {
		long min = values[0];
		long max = values[0];

		for (int j = 1; j < values.length; j++) {
			if (values[j] < min) {
				min = values[j];
			}
			if (values[j] > max) {
				max = values[j];
			}
		}
		boolean highlyCompressable = min + max < MIN_MAP_BLOCK_SIZE;
		return highlyCompressable;
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
