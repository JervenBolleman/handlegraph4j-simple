/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Spliterator;
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

    private int size = 0;
    private long[] edges = new long[4096];

    public void add(SimpleEdgeHandle eh) {
        edges[size++] = eh.left().id();
        edges[size++] = eh.right().id();
        growIfNeeded();
    }

    private void growIfNeeded() {
        if (size == edges.length) {
            int newSize = edges.length * 2;
            if (edges.length > FIXED_SIZE_INCREMENT) {
                newSize = edges.length + FIXED_SIZE_INCREMENT;
            }
            long[] newEdges = new long[newSize];
            System.arraycopy(edges, 0, newEdges, 0, edges.length);
            edges = newEdges;
        }
    }
    private static final int FIXED_SIZE_INCREMENT = 1024 * 1024 * 16;

    public void add(long left, long right) {
        edges[size++] = left;
        edges[size++] = right;
        growIfNeeded();
    }

    public void trimAndSort() {
        long[] newEdges = new long[size];
        System.arraycopy(edges, 0, newEdges, 0, size);
        edges = newEdges;
        sort();
    }

    public Stream<SimpleEdgeHandle> stream() {
        //start from zero do not early terminate
        var edgesIter = new EdgeHandleIteratorImpl(0);
        var spliterator = Spliterators.spliterator(edgesIter, edges.length, Spliterator.SIZED);
        return StreamSupport.stream(spliterator, false);
    }

    private void sort() {
        assert edges.length % 2 == 0;
        stupidSort();
    }

    /**
     * Quick implementation before implementing a proper in place sort.
     */
    private void stupidSort() {
        if (size != 0 && isNotSorted()) {
            edges = stupidSortInKeyOrder();
            stupidSortKeyValueOrder();
        }
    }

    private void stupidSortKeyValueOrder() {
        long currentKey = edges[0];
        int currentIndex = 0;
        for (int i = 2; i < edges.length; i += 2) {
            long key = edges[i];
            if (currentKey != key) {
                // if the key is no longer the same we have found stretch of 
                // values that need to be sorted. But sorting code only
                // activates if the stretch is longer than one (e.g. more than
                // once we have the same key.
                if (currentIndex + 2 != i) {
                    sortAStretchOfValues(currentIndex, i);
                }
                currentKey = key;
                currentIndex = i;
            }
        }
    }

    private void sortAStretchOfValues(int currentIndex, int i) {
        //we need to sort the values
        long[] valuesToSort = new long[(i - currentIndex) / 2];
        for (int k = 0, l = currentIndex + 1; l < i; l += 2, k++) {
            valuesToSort[k] = edges[l];
        }
        Arrays.sort(valuesToSort);
        for (int k = 0, l = currentIndex + 1; l < i; l += 2, k++) {
            edges[l] = valuesToSort[k];
        }

    }

    private long[] stupidSortInKeyOrder() {
        long[] keys = new long[edges.length / 2];
        for (int i = 0, j = 0; j < edges.length; i++, j += 2) {
            keys[i] = edges[j];
        }
        Arrays.sort(keys);
        long[] newEdges = new long[edges.length];
        int[] keyCount = new int[edges.length / 2];
        for (int i = 0; i < edges.length;) {
            long key = edges[i++];
            int keyIndex = Arrays.binarySearch(keys, key);
            int newIndex = keyIndex * 2;
            int name = keyCount[keyIndex];
            int suboffset = name * 2;
            keyCount[keyIndex] = ++name;
            newEdges[newIndex + suboffset] = key;
            newEdges[newIndex + suboffset + 1] = edges[i++];
        }
        return newEdges;
    }

    public Stream<SimpleEdgeHandle> streamToLeft(SimpleNodeHandle left) {
        int leftIndexStart = binarySearch0(edges, 0, size, left.id());
        var spliterator = Spliterators.spliterator(new EdgeHandleIteratorImpl(leftIndexStart), 0, 0);
        return StreamSupport.stream(spliterator, false);
    }

    private static int binarySearch0(long[] a, int fromIndex, int toIndex,
            long key) {
        int low = fromIndex;
        int high = toIndex - 2;

        while (low <= high) {
            int mid = (low + high) >>> 2;
            long midVal = a[mid];

            if (midVal < key) {
                low = mid + 2;
            } else if (midVal > key) {
                high = mid - 2;
            } else {
                return mid; // key found
            }
        }
        return -(low + 2);  // key not found.
    }

    public Stream<SimpleEdgeHandle> streamToRight(SimpleNodeHandle right) {
        return stream().filter(e -> e.right().equals(right));
    }

    public void resize(int maxEdges) {
        long[] newEdges = new long[maxEdges * 2];
        System.arraycopy(edges, 0, newEdges, 0, newEdges.length);
        edges = newEdges;
    }

    boolean isNotSorted() {
        long prevKey = edges[0];
        long prevValue = edges[1];
        for (int i = 2; i < edges.length;) {
            long currentKey = edges[i++];
            long currentValue = edges[i++];
            if (currentKey < prevKey) {
                return true;
            } else if (currentKey == prevKey) {
                if (currentValue < prevValue) {
                    return true;
                }
            }
            prevKey = currentKey;
            prevValue = currentValue;
        }
        return false;
    }

    private class EdgeHandleIteratorImpl implements Iterator<SimpleEdgeHandle> {

        int cursor = 0;
        final int max = size;

        public EdgeHandleIteratorImpl(int start) {
            cursor = start;
        }

        @Override
        public boolean hasNext() {
            return cursor < max;
        }

        @Override
        public SimpleEdgeHandle next() {
            int left = cursor++;
            int right = cursor++;
            SimpleEdgeHandle next = new SimpleEdgeHandle(edges[left], edges[right]);
            return next;
        }
    }
}
