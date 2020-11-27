/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sib.swiss.swissprot.handlegraph4j.simple.builders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.collections.api.list.primitive.LongList;
import sib.swiss.swissprot.handlegraph4j.simple.datastructures.SimpleEdgeList;
import sib.swiss.swissprot.handlegraph4j.simple.SimplePathGraph;
import sib.swiss.swissprot.handlegraph4j.simple.SimplePathHandle;
import sib.swiss.swissprot.handlegraph4j.simple.SimpleStepHandle;
import sib.swiss.swissprot.handlegraph4j.simple.datastructures.NodeToSequenceMap;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
class SimplePathGraphBuilder {

    final NodeToSequenceMap nodeToSequenceMap = new NodeToSequenceMap();
    final Map<String, SimplePathHandle> paths = new HashMap<>();
    final Map<SimplePathHandle, LongList> pathsToSteps = new HashMap<>();
    final SimpleEdgeList edges = new SimpleEdgeList();

    public SimplePathGraph build() {
        return new SimplePathGraph(nodeToSequenceMap, paths, pathsToSteps, edges);
    }
}
