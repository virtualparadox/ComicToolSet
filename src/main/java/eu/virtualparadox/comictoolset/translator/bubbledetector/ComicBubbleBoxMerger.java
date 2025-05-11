package eu.virtualparadox.comictoolset.translator.bubbledetector;

import java.util.ArrayList;
import java.util.List;

/**
 * A utility class for merging {@link ComicBubbleBox} instances
 * based purely on containment: if one box is mostly inside the other.
 */
public final class ComicBubbleBoxMerger {

    /**
     * Merges bounding boxes based on containment only.
     * Boxes are merged if either is at least {@code threshold} contained in the other.
     *
     * @param boxes     the list of detected {@link ComicBubbleBox} instances
     * @param threshold the containment threshold (e.g. 0.9f for 90%)
     * @return a list of merged {@link ComicBubbleBox} instances
     */
    public List<ComicBubbleBox> merge(final List<ComicBubbleBox> boxes,
                                      final float threshold) {
        final List<ComicBubbleBox> merged = new ArrayList<>();
        final boolean[] mergedFlag = new boolean[boxes.size()];

        for (int i = 0; i < boxes.size(); i++) {
            if (mergedFlag[i]) continue;

            final ComicBubbleBox base = boxes.get(i);
            final List<ComicBubbleBox> group = new ArrayList<>();
            group.add(base);
            mergedFlag[i] = true;

            for (int j = i + 1; j < boxes.size(); j++) {
                if (mergedFlag[j]) continue;
                final ComicBubbleBox other = boxes.get(j);

                if (isContainedBy(base, other) >= threshold ||
                        isContainedBy(other, base) >= threshold) {
                    group.add(other);
                    mergedFlag[j] = true;
                }
            }

            merged.add(mergeGroup(group));
        }

        return merged;
    }

    /**
     * Merges a group of bounding boxes into one bounding box that encloses all of them.
     * Confidence is averaged.
     *
     * @param group boxes to merge
     * @return enclosing {@link ComicBubbleBox}
     */
    private ComicBubbleBox mergeGroup(final List<ComicBubbleBox> group) {
        float x1 = Float.MAX_VALUE;
        float y1 = Float.MAX_VALUE;
        float x2 = Float.MIN_VALUE;
        float y2 = Float.MIN_VALUE;
        float sumConfidence = 0f;

        for (final ComicBubbleBox b : group) {
            x1 = Math.min(x1, b.x1());
            y1 = Math.min(y1, b.y1());
            x2 = Math.max(x2, b.x2());
            y2 = Math.max(y2, b.y2());
            sumConfidence += b.confidence();
        }

        final float avgConfidence = sumConfidence / group.size();
        return new ComicBubbleBox(x1, y1, x2, y2, avgConfidence, 0);
    }

    /**
     * Calculates what fraction of box {@code a}'s area is inside box {@code b}.
     *
     * @param a inner box
     * @param b outer box
     * @return value in [0.0, 1.0]
     */
    private float isContainedBy(final ComicBubbleBox a,
                                final ComicBubbleBox b) {
        final float xLeft = Math.max(a.x1(), b.x1());
        final float yTop = Math.max(a.y1(), b.y1());
        final float xRight = Math.min(a.x2(), b.x2());
        final float yBottom = Math.min(a.y2(), b.y2());

        if (xRight < xLeft || yBottom < yTop) {
            return 0f;
        }

        final float intersectionArea = (xRight - xLeft) * (yBottom - yTop);
        final float areaA = a.width() * a.height();
        return intersectionArea / areaA;
    }
}
