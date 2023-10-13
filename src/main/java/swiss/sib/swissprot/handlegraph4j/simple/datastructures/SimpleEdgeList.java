/**
 * Copyright (c) 2020, SIB Swiss Institute of Bioinformatics
 * and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleEdgeHandle;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;
import swiss.sib.swissprot.handlegraph4j.simple.functions.ToLong;

/**
 *
 @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 */
public class SimpleEdgeList {

    private final LongLongSpinalList<SimpleEdgeHandle> kv;

    public long size() {
        return kv.size();
    }

    public void writeToDisk(DataOutputStream raf) throws IOException {
        kv.toStream(raf);
    }
    
    public void open(RandomAccessFile raf) throws IOException {
        kv.fromStream(raf);
    }

    private class GetLeftId implements ToLong<SimpleEdgeHandle> {

        @Override
        public long apply(SimpleEdgeHandle t) {
            return t.left().id();
        }
    }

    private class GetRightId implements ToLong<SimpleEdgeHandle> {

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

    public AutoClosedIterator<SimpleEdgeHandle> iterateToLeft(SimpleNodeHandle left) {
        return kv.iterateWithKey(left.id());
    }

    public AutoClosedIterator<SimpleEdgeHandle> iterateToRight(SimpleNodeHandle right) {
        return kv.iterateWithValue(right.id());
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
