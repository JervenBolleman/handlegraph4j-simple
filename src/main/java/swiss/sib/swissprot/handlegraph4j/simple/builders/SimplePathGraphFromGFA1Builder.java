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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.collections.impl.map.mutable.primitive.ObjectLongHashMap;

import io.github.jervenbolleman.handlegraph4j.gfa1.GFA1Reader;
import io.github.jervenbolleman.handlegraph4j.gfa1.line.Line;
import io.github.jervenbolleman.handlegraph4j.gfa1.line.LinkLine;
import io.github.jervenbolleman.handlegraph4j.gfa1.line.PathLine;
import io.github.jervenbolleman.handlegraph4j.gfa1.line.PathLine.Step;
import io.github.jervenbolleman.handlegraph4j.gfa1.line.SegmentLine;
import io.github.jervenbolleman.handlegraph4j.sequences.Sequence;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleEdgeHandle;
import swiss.sib.swissprot.handlegraph4j.simple.SimplePathHandle;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.CompressedArrayBackedSteps;
import swiss.sib.swissprot.handlegraph4j.simple.datastructures.Steps;

/**
 *
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 */
public class SimplePathGraphFromGFA1Builder extends SimplePathGraphBuilder {

	public void parse(GFA1Reader gFA1Reader) {
		int pathId = 0;
		List<SegmentLine> postponedSegmentLines = new ArrayList<>();
		List<LinkLine> postponedLinkLines = new ArrayList<>();
		List<PathLine> postponedPathLines = new ArrayList<>();
		AtomicInteger edgeCount = new AtomicInteger();
		pathId = parseGFAFile(gFA1Reader, postponedSegmentLines, pathId, postponedPathLines, postponedLinkLines,
				edgeCount);
		long maxNodeId = nodeToSequenceMap.getMaxNodeId();

		ObjectLongHashMap<String> namesToNodeIds = new ObjectLongHashMap<>();
		parsePostPonedSegmentLines(maxNodeId, postponedSegmentLines, namesToNodeIds);

		nodeToSequenceMap.trim();

		parsePostPonedLinkLines(postponedLinkLines, namesToNodeIds);
	}

	private int parseGFAFile(GFA1Reader gFA1Reader, List<SegmentLine> postponedSegmentLines, int pathId,
			List<PathLine> postponedPathLines, List<LinkLine> postponedLinkLines,
			AtomicInteger edgeCount)
	{
		while (gFA1Reader.hasNext()) {
			Line next = gFA1Reader.next();
			switch (next.getCode()) {
			case SegmentLine.CODE:
				accept((SegmentLine) next, postponedSegmentLines);
				break;
			case PathLine.CODE:
				pathId = accept((PathLine) next, pathId, postponedPathLines);
				break;
			case LinkLine.CODE:
				accept((LinkLine) next, postponedLinkLines,
						// edgeWriter,
						edgeCount);
				break;
			}
		}
		return pathId;
	}

	private void parsePostPonedSegmentLines(long maxNodeId, List<SegmentLine> postponedSegmentLines,
			ObjectLongHashMap<String> namesToNodeIds) {
		ObjectLongHashMap<byte[]> namesToIdMap = new ObjectLongHashMap<>();
		for (SegmentLine sl : postponedSegmentLines) {
			long nextNodeId = maxNodeId++;
			namesToIdMap.put(sl.getName(), nextNodeId);
			nodeToSequenceMap.add(nextNodeId, sl.getSequence());
			namesToNodeIds.put(sl.getNameAsString(), nextNodeId);
		}
	}

	private void accept(SegmentLine segmentLine, List<SegmentLine> postponedSegmentLines) {

		try {
			long id = Long.parseLong(segmentLine.getNameAsString());
			Sequence sequence = segmentLine.getSequence();
			nodeToSequenceMap.add(id, sequence);
		} catch (NumberFormatException e) {
			postponedSegmentLines.add(segmentLine);
		}
	}

	private int accept(PathLine pathLine, int pathId, List<PathLine> postponedPathLines) {
		Iterator<Step> steps = pathLine.steps();
		int newPathId = pathId + 1;
		SimplePathHandle simplePathHandle = new SimplePathHandle(newPathId);
		paths.put(pathLine.getNameAsString(), simplePathHandle);
		long[] stepList = new long[pathLine.numberOfSteps()];
		int i = 0;
		while (steps.hasNext()) {
			Step step = steps.next();
			if (step.nodeHasLongId()) {
				if (step.isReverseComplement()) {
					stepList[i++] = -step.nodeLongId();
				} else {
					stepList[i++] = step.nodeLongId();
				}
			} else {
				postponedPathLines.add(pathLine);
				return newPathId;
			}
		}
		pathsToSteps.put(simplePathHandle, makeStepList(stepList));
		if (pathsToSteps.size() % 1000 == 0) {
			System.err.println("Parsed " + pathsToSteps.size() + " " + Instant.now() + " ");
		}
		return newPathId;
	}

	private Steps makeStepList(long[] values) {
		return new CompressedArrayBackedSteps(values);
	}

	private void accept(LinkLine ll, List<LinkLine> postponedLinkLines,
			AtomicInteger edgeCount)
	{
		try {
			String fromNameAsString = ll.getFromNameAsString();
			long fromNameAsLong = Long.parseLong(fromNameAsString);

			String toNameAsString = ll.getFromNameAsString();
			long toNameAsLong = Long.parseLong(toNameAsString);
			if (ll.isReverseComplimentOfFrom()) {
				fromNameAsLong = -fromNameAsLong;
			}
			if (ll.isReverseComplimentOfFrom()) {
				toNameAsLong = -toNameAsLong;
			}
			edgeCount.incrementAndGet();
			edges.add(new SimpleEdgeHandle(fromNameAsLong, toNameAsLong));
		} catch (NumberFormatException f) {
			postponedLinkLines.add(ll);
		}
	}

	private void parsePostPonedLinkLines(List<LinkLine> postponedLinkLines, ObjectLongHashMap<String> namesToNodeIds) {

		for (LinkLine ll : postponedLinkLines) {
			long fromNameAsLong = namesToNodeIds.get(ll.getFromNameAsString());
			long toNameAsLong = namesToNodeIds.get(ll.getToNameAsString());
			if (ll.isReverseComplimentOfFrom()) {
				fromNameAsLong = -fromNameAsLong;
			}
			if (ll.isReverseComplimentOfFrom()) {
				toNameAsLong = -toNameAsLong;
			}
			edges.add(new SimpleEdgeHandle(toNameAsLong, fromNameAsLong));
		}
	}
}
