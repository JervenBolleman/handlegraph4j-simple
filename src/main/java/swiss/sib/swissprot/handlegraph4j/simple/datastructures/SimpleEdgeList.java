/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import static swiss.sib.swissprot.handlegraph4j.simple.datastructures.LongLongSpinalList.ToLong;
import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleEdgeHandle;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class SimpleEdgeList {
    
    private final LongLongSpinalList<SimpleEdgeHandle> kv;
    
    private class GetLeftId implements ToLong<SimpleEdgeHandle>{

        @Override
        public long apply(SimpleEdgeHandle t) {
            return t.left().id();
        }
    }
    
    private class GetRightId implements ToLong<SimpleEdgeHandle>{

        @Override
        public long apply(SimpleEdgeHandle t) {
            return t.right().id();
        }
    }
    public SimpleEdgeList() {
        this.kv = new LongLongSpinalList<>(
                SimpleEdgeHandle::new,
                new GetLeftId(),
                new GetRightId(),
                new LeftThenRightOrder());
    }
    
    public void add(SimpleEdgeHandle eh) {
        kv.add(eh);
    }
    
    public void trimAndSort() {
        kv.trimAndSort();
    }
    
    public Iterator<SimpleEdgeHandle> iterator() {
        return kv.iterator();
    }
    
    public Stream<SimpleEdgeHandle> stream() {
        var si = Spliterators.spliteratorUnknownSize(kv.iterator(), 0);
        return StreamSupport.stream(si, false);
    }
    
    public Stream<SimpleEdgeHandle> streamToLeft(SimpleNodeHandle left) {
        return kv.streamToLeft(left.id());
    }
    
    public AutoClosedIterator<SimpleEdgeHandle> iterateToLeft(SimpleNodeHandle left) {
        return kv.iterateToLeft(left.id());
    }
    
    public Stream<SimpleEdgeHandle> streamToRight(SimpleNodeHandle right) {
        return stream().filter(e -> e.right().equals(right));
    }
    
    private static class LeftThenRightOrder implements Comparator<SimpleEdgeHandle> {
        
        public LeftThenRightOrder() {
        }
        
        @Override
        public int compare(SimpleEdgeHandle l, SimpleEdgeHandle r) {
            int cl = Long.compare(l.left().id(), r.left().id());
            if (cl == 0) {
                return Long.compare(l.right().id(), r.right().id());
            } else {
                return cl;
            }
        }
    }
}
