/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.from;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.map;
import java.util.PrimitiveIterator.OfLong;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.list.primitive.LongList;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class ListBackedSteps implements Steps {

    private final LongList nodesInRankOrder;

    public ListBackedSteps() {
        this.nodesInRankOrder = new LongArrayList();
    }

    public ListBackedSteps(LongList value) {
        this.nodesInRankOrder = value;
    }

    @Override
    public SimpleNodeHandle firstNode() {
        return new SimpleNodeHandle(nodesInRankOrder.get(0));
    }

    @Override
    public SimpleNodeHandle lastNode() {
        return new SimpleNodeHandle(nodesInRankOrder.get(nodesInRankOrder.size() - 1));
    }

    @Override
    public AutoClosedIterator<SimpleNodeHandle> nodes() {
        LongIterator longIterator = nodesInRankOrder.longIterator();
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
        return nodesInRankOrder.size();
    }

    @Override
    public long nodeIdOfStep(long rank) {
        return nodesInRankOrder.get((int) rank);
    }
    
    
}
