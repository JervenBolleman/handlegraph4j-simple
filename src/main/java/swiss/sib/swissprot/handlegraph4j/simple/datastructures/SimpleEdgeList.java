/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import java.util.Comparator;
import java.util.stream.Stream;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleEdgeHandle;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class SimpleEdgeList {
    private final LongLongSpinalList<SimpleEdgeHandle> kv;

    public SimpleEdgeList() {
        this.kv = new LongLongSpinalList<>(
                SimpleEdgeHandle::new,
                s -> s.left().id(),
                s -> s.right().id(),
                new LeftThenRightOrder());
    }

    public void add(SimpleEdgeHandle eh) {
        kv.add(eh);
    }

    public void trimAndSort() {
        kv.trimAndSort();
    }

    public Stream<SimpleEdgeHandle> stream() {
        return kv.stream();
    }

    public Stream<SimpleEdgeHandle> streamToLeft(SimpleNodeHandle left) {
        return kv.streamToLeft(left.id());
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
