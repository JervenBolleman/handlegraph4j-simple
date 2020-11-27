/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sib.swiss.swissprot.handlegraph4j.simple.builders;

import io.github.vgteam.handlegraph4j.gfa1.GFA1Reader;
import io.github.vgteam.handlegraph4j.gfa1.line.Line;
import io.github.vgteam.handlegraph4j.gfa1.line.LinkLine;
import io.github.vgteam.handlegraph4j.gfa1.line.PathLine;
import io.github.vgteam.handlegraph4j.gfa1.line.PathLine.Step;
import io.github.vgteam.handlegraph4j.gfa1.line.SegmentLine;
import io.github.vgteam.handlegraph4j.sequences.Sequence;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectLongHashMap;
import sib.swiss.swissprot.handlegraph4j.simple.SimplePathHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class SimplePathGraphFromGFA1Builder extends SimplePathGraphBuilder {

    public void parse(GFA1Reader gFA1Reader) {
        int pathId = 0;
        List<SegmentLine> postponedSegmentLines = new ArrayList<>();
        List<LinkLine> postponedLinkLines = new ArrayList<>();
        List<PathLine> postponedPathLines = new ArrayList<>();
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
                    accept((LinkLine) next, postponedLinkLines);
                    break;
            }
        }
        long maxNodeId = nodeToSequenceMap.getMaxNodeId();
        ObjectLongHashMap<String> namesToNodeIds = new ObjectLongHashMap<>();
        parsePostPonedSegmentLines(maxNodeId, postponedSegmentLines, namesToNodeIds);
        parsePostPonedLinkLines(postponedLinkLines, namesToNodeIds);
    }

    private void parsePostPonedSegmentLines(long maxNodeId, List<SegmentLine> postponedSegmentLines, ObjectLongHashMap<String> namesToNodeIds) {
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
        int newPathId = pathId++;
        SimplePathHandle simplePathHandle = new SimplePathHandle(newPathId);
        paths.put(pathLine.getNameAsString(), simplePathHandle);
        MutableLongList stepList = new LongArrayList();
        while (steps.hasNext()) {
            Step step = steps.next();
            if (step.nodeHasIntId()) {
                stepList.add(step.nodeIntId());
            } else {
                postponedPathLines.add(pathLine);
                return newPathId;
            }
        }
        pathsToSteps.put(simplePathHandle, stepList);
        return newPathId;
    }

    private void accept(LinkLine ll, List<LinkLine> postponedLinkLines) {
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
            edges.add(toNameAsLong, fromNameAsLong);
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
            edges.add(toNameAsLong, fromNameAsLong);
        }
    }
}
