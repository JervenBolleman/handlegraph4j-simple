/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sib.swiss.swissprot.handlegraph4j.simple;

import io.github.vgteam.handlegraph4j.StepHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public final class SimpleStepHandle implements StepHandle{

    private final int pathId;
    private final long nodeId;
    private final long rank;

    public SimpleStepHandle(int pathId, long nodeId, long rank) {
        this.pathId = pathId;
        this.nodeId = nodeId;
        this.rank = rank;
        
    }
  
    int pathId(){
        return pathId;
    }
    
    long nodeId(){
        return nodeId;
    }
    
    long rank(){
        return rank;
    }
}
