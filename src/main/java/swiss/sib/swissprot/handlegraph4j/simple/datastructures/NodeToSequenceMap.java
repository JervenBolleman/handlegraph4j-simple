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
import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import io.github.jervenbolleman.handlegraph4j.NodeSequence;
import io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator;
import io.github.jervenbolleman.handlegraph4j.sequences.Sequence;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;

/**
 *
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 */
public interface NodeToSequenceMap {

	boolean areAllSequencesOneBaseLong();

	long count();

	long getMaxNodeId();

	Sequence getSequence(SimpleNodeHandle handle);

	int getSequenceLength(SimpleNodeHandle handle);

	PrimitiveIterator.OfLong nodeIdsIterator();

	Iterator<SimpleNodeHandle> nodeIterator();

	Iterator<NodeSequence<SimpleNodeHandle>> nodeWithSequenceIterator();

	Stream<SimpleNodeHandle> nodes();

	LongStream nodesIds();

	AutoClosedIterator<SimpleNodeHandle> nodesWithSequence(Sequence s);

	public void writeToDisk(DataOutputStream raf) throws IOException;

}
