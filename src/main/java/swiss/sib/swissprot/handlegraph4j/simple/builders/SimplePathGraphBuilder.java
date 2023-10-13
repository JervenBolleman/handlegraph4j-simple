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
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
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
