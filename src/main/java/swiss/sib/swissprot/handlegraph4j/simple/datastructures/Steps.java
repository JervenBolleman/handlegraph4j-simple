/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.datastructures;

import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public interface Steps {

    SimpleNodeHandle firstNode();

    SimpleNodeHandle lastNode();

    AutoClosedIterator<SimpleNodeHandle> nodes();

    public long length();

    public long nodeIdOfStep(long rank);

}
