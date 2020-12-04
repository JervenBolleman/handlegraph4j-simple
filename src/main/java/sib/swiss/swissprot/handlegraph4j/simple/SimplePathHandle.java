/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sib.swiss.swissprot.handlegraph4j.simple;

import io.github.vgteam.handlegraph4j.PathHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public final class SimplePathHandle implements PathHandle{

    private final int pathId;

    public SimplePathHandle(int pathId) {
        this.pathId = pathId;
    }

    public int id() {
        return pathId;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + this.pathId;
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
        final SimplePathHandle other = (SimplePathHandle) obj;
        if (this.pathId != other.pathId) {
            return false;
        }
        return true;
    }
    
    
}
