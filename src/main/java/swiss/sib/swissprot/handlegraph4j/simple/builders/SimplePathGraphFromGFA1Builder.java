/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.builders;

import io.github.vgteam.handlegraph4j.gfa1.GFA1Reader;
import io.github.vgteam.handlegraph4j.gfa1.line.Line;
import io.github.vgteam.handlegraph4j.gfa1.line.LinkLine;
import io.github.vgteam.handlegraph4j.gfa1.line.PathLine;
import io.github.vgteam.handlegraph4j.gfa1.line.PathLine.Step;
import io.github.vgteam.handlegraph4j.gfa1.line.SegmentLine;
import io.github.vgteam.handlegraph4j.sequences.Sequence;
import java.io.BufferedWriter;
import java.io.IOException;
import static java.nio.charset.StandardCharsets.US_ASCII;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectLongHashMap;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleEdgeHandle;
import swiss.sib.swissprot.handlegraph4j.simple.SimplePathHandle;

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
        AtomicInteger edgeCount = new AtomicInteger();
//        try {
//            Path tempFile = Files.createTempFile("edges", "tmp");
//            try ( BufferedWriter tempEdges = Files.newBufferedWriter(tempFile, US_ASCII)) {
        pathId = parseGFAFile(gFA1Reader,
                postponedSegmentLines,
                pathId,
                postponedPathLines,
                postponedLinkLines,
                //                        tempEdges,
                edgeCount);

//            }
        long maxNodeId = nodeToSequenceMap.getMaxNodeId();
        
        ObjectLongHashMap<String> namesToNodeIds = new ObjectLongHashMap<>();
        parsePostPonedSegmentLines(maxNodeId, postponedSegmentLines, namesToNodeIds);
        
        nodeToSequenceMap.trim();

//            reparseTempEdges(tempFile, edgeCount);
        parsePostPonedLinkLines(postponedLinkLines, namesToNodeIds);

//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }
    
    private void reparseTempEdges(Path tempFile, AtomicInteger edgeCount) throws IOException {
        
        preSort(tempFile);
//        edges.resize(edgeCount.get() + 1);
        try ( Stream<String> lines = Files.lines(tempFile, US_ASCII)) {
            Consumer<String> lineToEdge = l -> {
                int indexOf = l.indexOf('\t');
                long from = Long.parseLong(l, 0, indexOf, RADIX);
                long to = Long.parseLong(l, indexOf + 1, l.length(), RADIX);
                edges.add(new SimpleEdgeHandle(from, to));
            };
            lines.forEach(lineToEdge);
        }
        Files.delete(tempFile);
    }
    private static final int RADIX = 10;
    
    private int parseGFAFile(GFA1Reader gFA1Reader,
            List<SegmentLine> postponedSegmentLines,
            int pathId,
            List<PathLine> postponedPathLines,
            List<LinkLine> postponedLinkLines,
            //            BufferedWriter edgeWriter,
            AtomicInteger edgeCount) 
//            throws IOException
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
                            //                            edgeWriter, 
                            edgeCount);
                    break;
            }
        }
        return pathId;
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
        int newPathId = pathId+1;
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
    
    private void accept(LinkLine ll,
            List<LinkLine> postponedLinkLines,
//            BufferedWriter edgeWriter,
            AtomicInteger edgeCount) 
//            throws IOException 
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
//            edgeWriter.append(Long.toString(toNameAsLong))
//                    .append('\t')
//                    .append(Long.toString(fromNameAsLong))
//                    .append('\n');
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
    
    private void preSort(Path tempFile) {
        try {
            String[] preSort = new String[]{"sort", "-n", "-k1", "-k2", tempFile.toString()};
            Process exec = Runtime.getRuntime().exec(preSort);
            boolean done = false;
            try {
                while (!done) {
                    int waitFor = exec.waitFor();
                    if (waitFor != 0) {
                        throw new RuntimeException("Expected sort to finish ok");
                    }
                    done = true;
                }
            } catch (InterruptedException ex) {
                Thread.interrupted();
            }
        } catch (IOException ex) {
            throw new RuntimeException("Expected sort to finish ok");
        }
    }
}
