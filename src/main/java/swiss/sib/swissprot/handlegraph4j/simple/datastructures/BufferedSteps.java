/*
 * /**
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

import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.from;
import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.map;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.PrimitiveIterator.OfLong;

import io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;

/**
 *
 @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
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

	public static BufferedSteps read(RandomAccessFile raf) throws IOException {
		long noOfSteps = raf.readLong();
	    MappedByteBuffer map = raf.getChannel().map(FileChannel.MapMode.READ_ONLY, raf.getFilePointer(), noOfSteps * Long.BYTES);
	    raf.seek(raf.getFilePointer() + noOfSteps * Long.BYTES);
	    var steps = new BufferedSteps(map.asLongBuffer());
		return steps;
	}
}
