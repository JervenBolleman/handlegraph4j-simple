/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple;

import io.github.vgteam.handlegraph4j.NodeHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public final class SimpleNodeHandle implements NodeHandle {

    private final long nodeId;

    public SimpleNodeHandle(long nodeId) {
        this.nodeId = nodeId;
    }

    public long id() {
        return nodeId;
    }

    public boolean isReverse() {
        return nodeId < 0;
    }

    public SimpleNodeHandle flip() {
        return new SimpleNodeHandle(-nodeId);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(nodeId);
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
        final SimpleNodeHandle other = (SimpleNodeHandle) obj;
        if (this.nodeId != other.nodeId) {
            return false;
        }
        return true;
    }

}
