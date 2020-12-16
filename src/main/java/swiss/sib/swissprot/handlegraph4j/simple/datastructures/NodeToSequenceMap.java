/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import io.github.vgteam.handlegraph4j.NodeSequence;
import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import io.github.vgteam.handlegraph4j.sequences.Sequence;
import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public interface NodeToSequenceMap {

    boolean areAllSequencesOneBaseLong();

    long count();

    long getMaxNodeId();

    Sequence getSequence(SimpleNodeHandle handle);

    PrimitiveIterator.OfLong nodeIdsIterator();

    Iterator<SimpleNodeHandle> nodeIterator();

    Iterator<NodeSequence<SimpleNodeHandle>> nodeWithSequenceIterator();

    Stream<SimpleNodeHandle> nodes();

    LongStream nodesIds();

    AutoClosedIterator<SimpleNodeHandle> nodesWithSequence(Sequence s);
    
}
