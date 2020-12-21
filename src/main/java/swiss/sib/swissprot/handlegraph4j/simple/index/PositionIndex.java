/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.index;

import java.util.stream.LongStream;
import swiss.sib.swissprot.handlegraph4j.simple.SimplePathHandle;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleStepHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public interface PositionIndex {

    public int endPositionOfStep(SimpleStepHandle s);

    public int beginPositionOfStep(SimpleStepHandle s);
    
    public LongStream positionsOf(SimplePathHandle p);
}
