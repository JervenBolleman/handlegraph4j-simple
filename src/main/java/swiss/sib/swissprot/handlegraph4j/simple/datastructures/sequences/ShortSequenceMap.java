/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures.sequences;

import io.github.vgteam.handlegraph4j.NodeSequence;
import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.empty;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.flatMap;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.map;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.concat;
import io.github.vgteam.handlegraph4j.iterators.CollectingOfLong;
import io.github.vgteam.handlegraph4j.sequences.Sequence;
import io.github.vgteam.handlegraph4j.sequences.SequenceType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PrimitiveIterator;
import java.util.PrimitiveIterator.OfLong;
import java.util.function.Function;
import java.util.function.LongFunction;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.BufferedNodeToSequenceMap;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class ShortSequenceMap implements NodeSequenceMap {
	// 32 is the normal max sequence length of VG
	private static final int MAX_FOR_SHORT_SEQUENCE = 32;
	final Map<Sequence, List<RoaringBitmap>> fewNps = new HashMap<>();

	public ShortSequenceMap() {
		initOneToThreeLengthSequences();
		initUnknownSequences();
	}

	void initUnknownSequences() {
		for (int i = 4; i < MAX_FOR_SHORT_SEQUENCE; i++) {
			byte[] nn = new byte[i];
			Arrays.fill(nn, (byte) 'n');
			RoaringBitmap roaring64Bitmap = new RoaringBitmap();
			var bitmaps = new ArrayList<RoaringBitmap>();
			bitmaps.add(roaring64Bitmap);
			fewNps.put(SequenceType.fromByteArray(nn), bitmaps);
		}
	}

	private void initOneToThreeLengthSequences() {
		// Generate all possible sequences length 1 to 3.
		for (Character ch1 : Sequence.KNOWN_IUPAC_CODES) {
			byte b1 = (byte) ch1.charValue();
			initSequenceAndBitmaps(b1);
			for (Character ch2 : Sequence.KNOWN_IUPAC_CODES) {
				byte b2 = (byte) ch2.charValue();
				initSequenceAndBitmaps(b1, b2);
				for (Character ch3 : Sequence.KNOWN_IUPAC_CODES) {
					byte b3 = (byte) ch3.charValue();
					initSequenceAndBitmaps(b1, b2, b3);
				}
			}
		}
	}

	private void initSequenceAndBitmaps(byte... b1) {
		Sequence seq = SequenceType.fromByteArray(b1);
		var bitmaps = new ArrayList<RoaringBitmap>();
		bitmaps.add(new RoaringBitmap());
		fewNps.put(seq, bitmaps);
	}

	@Override
	public boolean containsSequence(Sequence s) {
		return fewNps.containsKey(s) || fewNps.containsKey(s.reverseComplement());
	}

	@Override
	public void add(long id, Sequence sequence) {
		int index = (int) (id >>> MAX_FOR_SHORT_SEQUENCE);
		fewNps.get(sequence).get(index).add((int) id);
	}

	@Override
	public void trim() {
		Iterator<Entry<Sequence, List<RoaringBitmap>>> it = fewNps.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Sequence, List<RoaringBitmap>> en = it.next();
			if (en.getValue().isEmpty()) {
				it.remove();
			} else {
				for (RoaringBitmap rb : en.getValue()) {
					rb.runOptimize();
				}
			}
		}
	}

	@Override
	public long size() {
		return fewNps.values().stream().flatMap(List::stream).mapToLong(RoaringBitmap::getLongCardinality).sum();
	}

	@Override
	public int maxSequenceLength() {
		return fewNps.keySet().stream().mapToInt(Sequence::length).max().orElse(0);
	}

	@Override
	public AutoClosedIterator<SimpleNodeHandle> nodeWithSequences(Sequence s) {
		return concat(forwardNodeWithSequences(s), reverseNodeWithSequences(s));
	}

	private AutoClosedIterator<SimpleNodeHandle> forwardNodeWithSequences(Sequence s) {
		List<RoaringBitmap> get = fewNps.get(s);
		if (get.isEmpty()) {
			return empty();
		} else {
			PrimitiveIterator.OfLong nodeids = iteratorFromBitMaps(get);
			LongFunction<SimpleNodeHandle> name = SimpleNodeHandle::new;
			return map(nodeids, name);
		}
	}

	private AutoClosedIterator<SimpleNodeHandle> reverseNodeWithSequences(Sequence s) {
		List<RoaringBitmap> get = fewNps.get(s.reverseComplement());
		if (get.isEmpty()) {
			return empty();
		} else {
			PrimitiveIterator.OfLong nodeids = iteratorFromBitMaps(get);
			LongFunction<SimpleNodeHandle> name = (id) -> new SimpleNodeHandle(-id);
			return map(nodeids, name);
		}
	}

	@Override
	public boolean isEmpty() {
		return fewNps.values().stream().flatMap(List::stream).anyMatch(r -> !r.isEmpty());
	}

	private class SequenceAndRoaringBitMap implements
			Function<Map.Entry<Sequence, List<RoaringBitmap>>, AutoClosedIterator<NodeSequence<SimpleNodeHandle>>> {

		@Override
		public AutoClosedIterator<NodeSequence<SimpleNodeHandle>> apply(Map.Entry<Sequence, List<RoaringBitmap>> en) {
			Sequence s = en.getKey();
			List<RoaringBitmap> bitmaps = en.getValue();
			if (bitmaps.isEmpty()) {
				return empty();
			}
			PrimitiveIterator.OfLong nodeids = iteratorFromBitMaps(bitmaps);
			return map(nodeids, new BufferedNodeToSequenceMap.AttachNodeToSequence(s));
		}

	}

	@Override
	public AutoClosedIterator<NodeSequence<SimpleNodeHandle>> nodeSequences() {
		var smalls = flatMap(map(fewNps.entrySet().iterator(), new SequenceAndRoaringBitMap()));
		return smalls;
	}

	@Override
	public OfLong nodeIds() {
		var lists = map(fewNps.entrySet().iterator(), Entry::getValue);
		var nodes = map(lists, ShortSequenceMap::iteratorFromBitMaps);
		return new CollectingOfLong(nodes);
	}

	private static PrimitiveIterator.OfLong iteratorFromBitMaps(List<RoaringBitmap> bitmaps) {
		PrimitiveIterator.OfLong nodeids = new PrimitiveIterator.OfLong() {
			long index = 0;
			PeekableIntIterator current = bitmaps.get(0).getIntIterator();

			@Override
			public long nextLong() {
				long id = (long) current.next();
				return id | (index << Integer.BYTES * 8);
			}

			@Override
			public boolean hasNext() {
				if (current.hasNext()) {
					return true;
				}
				while (index < bitmaps.size() - 2 && !current.hasNext()) {
					index++;
					current = bitmaps.get((int) index).getIntIterator();
					if (current.hasNext()) {
						return true;
					}
				}
				return false;
			}
		};
		return nodeids;
	}

	@Override
	public Sequence getSequence(long id) {
		int index = (int) (id >>> MAX_FOR_SHORT_SEQUENCE);
		for (Entry<Sequence, List<RoaringBitmap>> en : fewNps.entrySet()) {
			if (en.getValue().size() > index) {
				RoaringBitmap rb = en.getValue().get(index);
				if (rb.contains((int) id)) {
					return en.getKey();
				}
			}
		}
		return null;
	}
}
