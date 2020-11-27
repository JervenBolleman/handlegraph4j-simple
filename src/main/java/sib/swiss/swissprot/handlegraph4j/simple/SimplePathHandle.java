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

    int id() {
        return pathId;
    }
}
