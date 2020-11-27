/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sib.swiss.swissprot.handlegraph4j.simple.datastructures;

import io.github.vgteam.handlegraph4j.iterators.EdgeHandleIterator;
import sib.swiss.swissprot.handlegraph4j.simple.SimpleEdgeHandle;
import sib.swiss.swissprot.handlegraph4j.simple.SimpleNodeHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class SimpleEdgeList {

    private int size = 0;
    private long[] edges = new long[4096];

    public void add(SimpleEdgeHandle eh) {
        edges[size++] = eh.left().id();
        edges[size++] = eh.right().id();
        growIfNeeded();
    }

    private void growIfNeeded() {
        if (size == edges.length) {
            long[] newEdges = new long[edges.length * 2];
            System.arraycopy(edges, 0, newEdges, 0, edges.length);
            edges = newEdges;
        }
    }

    public void add(long left, long right) {
        edges[size++] = left;
        edges[size++] = right;
        growIfNeeded();
    }

    public EdgeHandleIterator<SimpleNodeHandle, SimpleEdgeHandle> iterator() {
        return new EdgeHandleIterator<>() {
            final int max = size;
            final long[] edgeCopu = edges;
            int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < max;
            }

            @Override
            public SimpleEdgeHandle next() {
                long left = cursor++;
                long right = cursor++;
                return new SimpleEdgeHandle(left, right);
            }

            @Override
            public void close() throws Exception {

            }

        };
    }
}
