/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sib.swiss.swissprot.handlegraph4j.simple;

import io.github.vgteam.handlegraph4j.EdgeHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class SimpleEdgeHandle implements EdgeHandle<SimpleNodeHandle> {

    private final long left;
    private final long right;

    public SimpleEdgeHandle(SimpleNodeHandle left, SimpleNodeHandle right) {
        this.left = left.id();
        this.right = right.id();
    }

    public SimpleEdgeHandle(long left, long right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public SimpleNodeHandle right() {
        return new SimpleNodeHandle(right);
    }

    @Override
    public SimpleNodeHandle left() {
        return new SimpleNodeHandle(left);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + (int) (this.left ^ (this.left >>> 32));
        hash = 83 * hash + (int) (this.right ^ (this.right >>> 32));
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
        final SimpleEdgeHandle other = (SimpleEdgeHandle) obj;
        if (this.left != other.left) {
            return false;
        }
        if (this.right != other.right) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "SimpleEdgeHandle{" + "left=" + left + ", right=" + right + '}';
    }
}
