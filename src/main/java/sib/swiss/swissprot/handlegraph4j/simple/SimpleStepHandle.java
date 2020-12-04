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

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 47 * hash + this.pathId;
        hash = 47 * hash + (int) (this.nodeId ^ (this.nodeId >>> 32));
        hash = 47 * hash + (int) (this.rank ^ (this.rank >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SimpleStepHandle other = (SimpleStepHandle) obj;
        if (this.pathId != other.pathId) {
            return false;
        }
        if (this.nodeId != other.nodeId) {
            return false;
        }
        if (this.rank != other.rank) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "SimpleStepHandle{" + "pathId=" + pathId + ", nodeId=" + nodeId + ", rank=" + rank + '}';
    }
}
