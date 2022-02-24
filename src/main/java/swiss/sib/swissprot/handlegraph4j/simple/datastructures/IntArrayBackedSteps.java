/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.from;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.map;

import java.util.Arrays;
import java.util.PrimitiveIterator.OfInt;
import java.util.PrimitiveIterator.OfLong;

import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class IntArrayBackedSteps implements Steps {

	private final int[]  nodesInRankOrder;

	public IntArrayBackedSteps(int[] values) {
		this.nodesInRankOrder = values;
	}

	@Override
	public SimpleNodeHandle firstNode() {
		return new SimpleNodeHandle(nodesInRankOrder[0]);
	}

	@Override
	public SimpleNodeHandle lastNode() {
		return new SimpleNodeHandle(nodesInRankOrder[nodesInRankOrder.length - 1]);
	}

	@Override
	public AutoClosedIterator<SimpleNodeHandle> nodes() {
		OfInt longIterator = Arrays.stream(nodesInRankOrder).iterator();
		OfLong n = new OfLong() {
			@Override
			public long nextLong() {
				return longIterator.next();
			}

			@Override
			public boolean hasNext() {
				return longIterator.hasNext();
			}

		};
		return map(from(n), SimpleNodeHandle::new);
	}

	@Override
	public long length() {
		return nodesInRankOrder.length;
	}

	@Override
	public long nodeIdOfStep(long rank) {
		return nodesInRankOrder[((int) rank)];
	}

}
