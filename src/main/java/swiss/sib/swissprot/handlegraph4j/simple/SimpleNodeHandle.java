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

import io.github.jervenbolleman.handlegraph4j.NodeHandle;

/**
 *
 @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
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
