/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.from;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.map;
import java.nio.LongBuffer;
import java.util.PrimitiveIterator.OfLong;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class BufferedSteps implements Steps {

    public BufferedSteps(LongBuffer nodesInRankOrder) {
        this.nodesInRankOrder = nodesInRankOrder;
    }

    private final LongBuffer nodesInRankOrder;

    @Override
    public SimpleNodeHandle firstNode() {
        return new SimpleNodeHandle(nodesInRankOrder.get(0));
    }

    @Override
    public SimpleNodeHandle lastNode() {
        return new SimpleNodeHandle(nodesInRankOrder.get(nodesInRankOrder.limit() - 1));
    }

    @Override
    public AutoClosedIterator<SimpleNodeHandle> nodes() {
        OfLong n = new OfLong() {
            private int current;

            @Override
            public long nextLong() {
                return nodesInRankOrder.get(current++);
            }

            @Override
            public boolean hasNext() {
                return current < nodesInRankOrder.limit() - 1;
            }

        };
        return map(from(n), SimpleNodeHandle::new);
    }

    @Override
    public long length() {
        return nodesInRankOrder.limit();
    }

    @Override
    public long nodeIdOfStep(long rank) {
        return nodesInRankOrder.get((int) rank);
    }
}
