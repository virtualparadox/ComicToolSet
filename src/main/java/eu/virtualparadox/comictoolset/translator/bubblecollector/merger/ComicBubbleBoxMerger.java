package eu.virtualparadox.comictoolset.translator.bubblecollector.merger;

import eu.virtualparadox.comictoolset.translator.bubblecollector.ComicBubbleBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A utility class for merging {@link ComicBubbleBox} instances
 * based on spatial containment or significant overlap. If two boxes
 * overlap more than a given threshold (relative to the smaller box),
 * they are merged into a single box. The merging process repeats until
 * no more boxes can be combined.
 */
public final class ComicBubbleBoxMerger {

    /**
     * Merges overlapping or contained {@link ComicBubbleBox} instances based on a given overlap threshold.
     *
     * @param originalBoxes the original list of bounding boxes to merge
     * @param threshold     the minimum overlap ratio (e.g., 0.5 = 50%) for merging boxes,
     *                      relative to the smaller of the two areas
     * @return a new list of merged bounding boxes with reduced redundancy
     */
    public List<ComicBubbleBox> merge(final List<ComicBubbleBox> originalBoxes, final float threshold) {
        final List<ComicBubbleBox> boxes = new ArrayList<>(originalBoxes);

        boolean restart;
        do {
            restart = false;
            final List<IntPair> toMerge = new ArrayList<>();

            for (int i = 0; i < boxes.size(); i++) {
                for (int j = i + 1; j < boxes.size(); j++) {
                    final ComicBubbleBox box1 = boxes.get(i);
                    final ComicBubbleBox box2 = boxes.get(j);
                    if (overlaps(box1, box2, threshold)) {
                        toMerge.add(new IntPair(i, j));
                    }
                }
            }

            final Set<Integer> used = new HashSet<>();
            final List<Integer> toRemove = new ArrayList<>();

            for (final IntPair pair : toMerge) {
                if (used.contains(pair.first) || used.contains(pair.second)) {
                    continue;
                }
                used.add(pair.first);
                used.add(pair.second);

                final ComicBubbleBox box1 = boxes.get(pair.first);
                final ComicBubbleBox box2 = boxes.get(pair.second);

                boxes.set(pair.first, merge(box1, box2));
                toRemove.add(pair.second);

                restart = true;
            }

            // Remove boxes from highest index to lowest to avoid shifting
            toRemove.sort((a, b) -> Integer.compare(b, a));
            for (final int index : toRemove) {
                boxes.remove(index);
            }

        } while (restart);

        return boxes;
    }

    /**
     * Determines whether two bounding boxes overlap significantly based on the threshold.
     * The overlap ratio is calculated as (intersection area / smaller box area).
     *
     * @param a         the first box
     * @param b         the second box
     * @param threshold the minimum overlap ratio required to consider the boxes overlapping
     * @return true if the boxes overlap more than the threshold, false otherwise
     */
    public boolean overlaps(final ComicBubbleBox a, final ComicBubbleBox b, final float threshold) {
        final int xOverlap = Math.max(0, Math.min(a.x2(), b.x2()) - Math.max(a.x1(), b.x1()));
        final int yOverlap = Math.max(0, Math.min(a.y2(), b.y2()) - Math.max(a.y1(), b.y1()));
        final int overlapArea = xOverlap * yOverlap;

        final int areaA = a.width() * a.height();
        final int areaB = b.width() * b.height();
        final int minArea = Math.min(areaA, areaB);

        final float ratio = (float) overlapArea / minArea;
        return ratio > threshold;
    }

    /**
     * Merges two {@link ComicBubbleBox} instances into one that fully contains both.
     * The resulting box has averaged confidence and the higher class ID.
     *
     * @param a the first box
     * @param b the second box
     * @return a new {@link ComicBubbleBox} that spans both input boxes
     */
    public ComicBubbleBox merge(final ComicBubbleBox a, final ComicBubbleBox b) {
        final int newX1 = Math.min(a.x1(), b.x1());
        final int newY1 = Math.min(a.y1(), b.y1());
        final int newX2 = Math.max(a.x2(), b.x2());
        final int newY2 = Math.max(a.y2(), b.y2());
        final float newConfidence = (a.confidence() + b.confidence()) / 2.0f;
        final int newClassId = Math.max(a.classId(), b.classId());
        return new ComicBubbleBox(newX1, newY1, newX2, newY2, newConfidence, newClassId);
    }
}
