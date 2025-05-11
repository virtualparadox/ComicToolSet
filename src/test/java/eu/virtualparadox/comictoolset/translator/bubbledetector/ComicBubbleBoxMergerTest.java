package eu.virtualparadox.comictoolset.translator.bubbledetector;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.List;

class ComicBubbleBoxMergerTest {

    private final ComicBubbleBoxMerger merger = new ComicBubbleBoxMerger();

    @Test
    void testNoOverlap_NoMerge() {
        List<ComicBubbleBox> input = List.of(
                new ComicBubbleBox(0, 0, 100, 100, 0.9f, 0),
                new ComicBubbleBox(200, 200, 300, 300, 0.85f, 0)
        );
        List<ComicBubbleBox> result = merger.merge(input, 0.9f);

        assertEquals(2, result.size());
        assertTrue(result.containsAll(input));
    }

    @Test
    void testExactOverlap_Merge() {
        List<ComicBubbleBox> input = List.of(
                new ComicBubbleBox(10, 10, 110, 110, 0.9f, 0),
                new ComicBubbleBox(10, 10, 110, 110, 0.8f, 0)
        );
        List<ComicBubbleBox> result = merger.merge(input, 0.9f);

        assertEquals(1, result.size());
        ComicBubbleBox merged = result.get(0);
        assertEquals(10f, merged.x1(), 0.01);
        assertEquals(110f, merged.x2(), 0.01);
        assertEquals((0.9f + 0.8f) / 2, merged.confidence(), 0.01);
    }

    @Test
    void testHighIoU_Merge() {
        List<ComicBubbleBox> input = List.of(
                new ComicBubbleBox(50, 50, 150, 150, 0.9f, 0),
                new ComicBubbleBox(60, 60, 140, 140, 0.7f, 0)
        );
        List<ComicBubbleBox> result = merger.merge(input, 0.5f);

        assertEquals(1, result.size());
        ComicBubbleBox merged = result.get(0);
        assertEquals(50f, merged.x1(), 0.01);
        assertEquals(150f, merged.x2(), 0.01);
        assertEquals((0.9f + 0.7f) / 2, merged.confidence(), 0.01);
    }

    @Test
    void testLowIoU_NoMerge() {
        List<ComicBubbleBox> input = List.of(
                new ComicBubbleBox(0, 0, 100, 100, 0.95f, 0),
                new ComicBubbleBox(101, 101, 200, 200, 0.75f, 0)
        );
        List<ComicBubbleBox> result = merger.merge(input, 0.5f);

        assertEquals(2, result.size());
    }

    @Test
    void testChainMerging() {
        List<ComicBubbleBox> input = List.of(
                new ComicBubbleBox(10, 10, 50, 50, 0.9f, 0),
                new ComicBubbleBox(11, 11, 80, 80, 0.8f, 0),
                new ComicBubbleBox(12, 12, 110, 110, 0.85f, 0)
        );
        List<ComicBubbleBox> result = merger.merge(input, 0.1f);

        assertEquals(1, result.size());
        ComicBubbleBox merged = result.get(0);
        assertEquals(10f, merged.x1(), 0.01);
        assertEquals(110f, merged.x2(), 0.01);
        assertEquals((0.9f + 0.8f + 0.85f) / 3, merged.confidence(), 0.01);
    }
}
