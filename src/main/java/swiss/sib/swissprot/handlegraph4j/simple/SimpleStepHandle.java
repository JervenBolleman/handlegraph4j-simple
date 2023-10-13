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
package swiss.sib.swissprot.handlegraph4j.simple;

import io.github.jervenbolleman.handlegraph4j.StepHandle;

/**
 *
 @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
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
        hash = 47 * hash + (int) (Long.hashCode(this.nodeId));
        hash = 47 * hash + (int) (Long.hashCode(this.rank));
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
