/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures.sequences;

import io.github.vgteam.handlegraph4j.NodeSequence;
import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import io.github.vgteam.handlegraph4j.sequences.Sequence;
import java.util.PrimitiveIterator;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public interface NodeSequenceMap {

	static final int NOT_FOUND = -404;
    
	void add(long id, Sequence sequence);

    Sequence getSequence(long id);
    
    default int getSequenceLength(long id) {
    	Sequence sequence = getSequence(id);
    	if (sequence != null)
    		return sequence.length();
    	else
    		return NOT_FOUND;
    }

    boolean isEmpty();

    PrimitiveIterator.OfLong nodeIds();

    AutoClosedIterator<NodeSequence<SimpleNodeHandle>> nodeSequences();

    long size();

    void trim();

    public AutoClosedIterator<SimpleNodeHandle> nodeWithSequences(Sequence s);

    public boolean containsSequence(Sequence s);

    public int maxSequenceLength();

}
