/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.builders;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.collections.api.list.primitive.LongList;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.SimpleEdgeList;
import swiss.sib.swissprot.handlegraph4j.simple.SimplePathGraph;
import swiss.sib.swissprot.handlegraph4j.simple.SimplePathHandle;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.NodeToSequenceMap;

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
        Map<SimplePathHandle, long[]> justpaths = new HashMap<>();
        var iterator = pathsToSteps.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<SimplePathHandle, LongList> next = iterator.next();
            justpaths.put(next.getKey(), next.getValue().toArray());
            iterator.remove();
        }
        nodeToSequenceMap.trim();
        return new SimplePathGraph(nodeToSequenceMap, paths, justpaths, edges);
    }
}
