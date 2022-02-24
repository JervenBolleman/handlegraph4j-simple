/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.builders;

import java.util.HashMap;
import java.util.Map;

import swiss.sib.swissprot.handlegraph4j.simple.Path;
import swiss.sib.swissprot.handlegraph4j.simple.SimplePathGraph;
import swiss.sib.swissprot.handlegraph4j.simple.SimplePathHandle;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.InMemoryNodeToSequenceMap;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.SimpleEdgeList;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.Steps;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
class SimplePathGraphBuilder {

    final InMemoryNodeToSequenceMap nodeToSequenceMap = new InMemoryNodeToSequenceMap();
    final Map<String, SimplePathHandle> paths = new HashMap<>();
    final Map<SimplePathHandle, Steps> pathsToSteps = new HashMap<>();
    final SimpleEdgeList edges = new SimpleEdgeList();

    public SimplePathGraph build() {
        Map<SimplePathHandle, Path> justpaths = new HashMap<>();
        var iterator = pathsToSteps.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<SimplePathHandle, Steps> next = iterator.next();
            Steps steps = next.getValue();

            SimplePathHandle pathHandle = next.getKey();
            var name = getPathName(pathHandle, paths);
            var path = new Path(name, pathHandle.id(), steps, nodeToSequenceMap);
            justpaths.put(next.getKey(), path);
            iterator.remove();
        }
        nodeToSequenceMap.trim();
        return new SimplePathGraph(nodeToSequenceMap, justpaths, edges);
    }

    private static String getPathName(SimplePathHandle ph, Map<String, SimplePathHandle> paths) {
        for (Map.Entry<String, SimplePathHandle> en : paths.entrySet()) {
            if (en.getValue().equals(ph)) {
                return en.getKey();
            }
        }
        return null;
    }
}
