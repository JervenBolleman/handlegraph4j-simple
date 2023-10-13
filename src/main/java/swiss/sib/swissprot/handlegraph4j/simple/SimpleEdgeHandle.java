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

import io.github.jervenbolleman.handlegraph4j.EdgeHandle;

/**
 *
 @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
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
