package swiss.sib.swissprot.handlegraph4j.simple.datastructures.chunks;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.PrimitiveIterator.OfLong;

public final class SearchChunk<T> implements Chunk<T> {

	private static final String NOT_SUPPORTED = "Not supported, this is just for the binary search.";
	private final long key;

	public SearchChunk(long key) {
		this.key = key;
	}

	@Override
	public long size() {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public boolean isFull() {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public void sort() {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public T first() {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public T last() {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public OfLong keyIterator() {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public OfLong valueIterator() {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public Iterator<T> iterator(int start) {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public long firstKey() {
		return key;
	}

	@Override
	public long lastKey() {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public void toStream(DataOutputStream stream) throws IOException {
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public Type getType() {
		return Type.SORT;
	}
}