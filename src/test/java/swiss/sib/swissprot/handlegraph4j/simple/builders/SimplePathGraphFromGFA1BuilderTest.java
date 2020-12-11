/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.handlegraph4j.simple.builders;

import io.github.vgteam.handlegraph4j.gfa1.GFA1Reader;
import java.util.Arrays;
import java.util.PrimitiveIterator;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import swiss.sib.swissprot.handlegraph4j.simple.SimplePathGraph;
import swiss.sib.swissprot.handlegraph4j.simple.SimplePathHandle;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleStepHandle;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class SimplePathGraphFromGFA1BuilderTest {

    private static final String TEST_DATA = "H\tVN:Z:1.0\n"
            + "S\t1\tCAAATAAG\n"
            + "S\t2\tA\n"
            + "S\t3\tG\n"
            + "S\t4\tT\n"
            + "S\t5\tC\n"
            + "S\t6\tTTG\n"
            + "S\t7\tA\n"
            + "S\t8\tG\n"
            + "S\t9\tAAATTTTCTGGAGTTCTAT\n"
            + "S\t10\tA\n"
            + "S\t11\tN\n"
            + "S\t12\tATAT\n"
            + "S\t13\tA\n"
            + "S\t14\tT\n"
            + "S\t15\tCCAACTCTCTG\n"
            + "P\tx\t1+,3+,5+,6+,8+,9+,11+,12+,14+,15+,3+\t8M,1M,1M,3M,1M,19M,1M,4M,1M,11M\n"
            + "P\ty\t1+,3+,5+,6+,8+,9+,11+,12+,14+,15+,1+\t8M,1M,1M,3M,1M,19M,1M,4M,1M,11M\n"
            + "L\t1\t+\t2\t+\t0M\n"
            + "L\t1\t+\t3\t+\t0M\n"
            + "L\t2\t+\t4\t+\t0M\n"
            + "L\t2\t+\t5\t+\t0M\n"
            + "L\t3\t+\t4\t+\t0M\n"
            + "L\t3\t+\t5\t+\t0M\n"
            + "L\t4\t+\t6\t+\t0M\n"
            + "L\t5\t+\t6\t+\t0M\n"
            + "L\t6\t+\t7\t+\t0M\n"
            + "L\t6\t+\t8\t+\t0M\n"
            + "L\t7\t+\t9\t+\t0M\n"
            + "L\t8\t+\t9\t+\t0M\n"
            + "L\t9\t+\t10\t+\t0M\n"
            + "L\t9\t+\t11\t+\t0M\n"
            + "L\t10\t+\t12\t+\t0M\n"
            + "L\t11\t+\t12\t+\t0M\n"
            + "L\t12\t+\t13\t+\t0M\n"
            + "L\t12\t+\t14\t+\t0M\n"
            + "L\t13\t+\t15\t+\t0M\n"
            + "L\t14\t+\t15\t+\t0M\n"
            + "L\t15\t+\t3\t+\t0M";

    public SimplePathGraphFromGFA1BuilderTest() {
    }

    /**
     * Test of parse method, of class SimplePathGraphFromGFA1Builder.
     */
    @Test
    public void testParse() {
        GFA1Reader gFA1Reader = new GFA1Reader(Arrays.asList(TEST_DATA.split("\n")).iterator());
        SimplePathGraphFromGFA1Builder instance = new SimplePathGraphFromGFA1Builder();
        instance.parse(gFA1Reader);
        SimplePathGraph graph = instance.build();
        assertFalse(graph.isEmpty());
        try ( var paths = graph.paths()) {
            assertTrue(paths.hasNext());
            SimplePathHandle path = paths.next();
            assertNotNull(path);
            assertEquals("x", graph.nameOfPath(path));

            try ( var steps = graph.stepsOf(path)) {
                int[] expectedNodeIds = new int[]{1, 3, 5, 6, 8, 9, 11, 12, 14, 15, 3};
                int expectedRank = 0;
                for (int expectedNodeId : expectedNodeIds) {
                    assertTrue(steps.hasNext());
                    SimpleStepHandle step1 = steps.next();
                    assertEquals((long) expectedNodeId, graph.nodeOfStep(step1).id());
                    assertEquals((long) expectedRank, graph.rankOfStep(step1));
                    expectedRank++;
                }
                assertFalse(steps.hasNext());
            }
            assertFalse(graph.isCircular(path));
            assertTrue(paths.hasNext());
            path = paths.next();
            assertNotNull(path);
            assertEquals("y", graph.nameOfPath(path));
            assertTrue(graph.isCircular(path));
        }
    }

    /**
     * Test of positionsOf method, of class SimplePathGraph.
     */
    @Test
    public void testPositionsOf() {
        GFA1Reader gFA1Reader = new GFA1Reader(Arrays.asList(TEST_DATA.split("\n")).iterator());
        SimplePathGraphFromGFA1Builder instance = new SimplePathGraphFromGFA1Builder();
        instance.parse(gFA1Reader);
        SimplePathGraph graph = instance.build();
        assertFalse(graph.isEmpty());
        try ( var paths = graph.paths()) {
            assertTrue(paths.hasNext());
            SimplePathHandle path = paths.next();
            assertNotNull(path);
            LongStream positionsOf = graph.positionsOf(path);
            PrimitiveIterator.OfLong iterator = positionsOf.iterator();
            int[] expectedPositions = new int[]{0, 8,
                9, 10,
                11, 12,
                13, 16,
                17, 18,
                19, 31,
                32, 33,
                34, 38,
                39, 40,
                41, 52,
                53, 54};
            for (int i = 0; i < expectedPositions.length; i++) {
                assertTrue(iterator.hasNext());
                assertEquals(expectedPositions[i], iterator.next());
            }
            assertFalse(iterator.hasNext());
        }
    }

    /**
     * Test of positionsOf method, of class SimplePathGraph.
     */
    @Test
    public void testNumberOfNodes() {
        GFA1Reader gFA1Reader = new GFA1Reader(Arrays.asList(TEST_DATA.split("\n")).iterator());
        SimplePathGraphFromGFA1Builder instance = new SimplePathGraphFromGFA1Builder();
        instance.parse(gFA1Reader);
        SimplePathGraph graph = instance.build();
        assertFalse(graph.isEmpty());
        try ( var nodes = graph.nodes()) {
            long count = 0;
            while (nodes.hasNext()) {
                assertNotNull(nodes.next());
                count++;
            }
            assertEquals(15, count);
        }
    }
}
