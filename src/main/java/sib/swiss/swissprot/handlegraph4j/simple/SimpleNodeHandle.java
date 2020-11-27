/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sib.swiss.swissprot.handlegraph4j.simple;

import io.github.vgteam.handlegraph4j.NodeHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public final class SimpleNodeHandle implements NodeHandle{

    private final long nodeId;

    public SimpleNodeHandle(long nodeId) {
        this.nodeId = nodeId;
    }

    public long id() {
        return nodeId;
    }
}
