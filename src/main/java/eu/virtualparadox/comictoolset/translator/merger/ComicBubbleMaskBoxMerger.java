package eu.virtualparadox.comictoolset.translator.merger;

import eu.virtualparadox.comictoolset.translator.textboxgenerator.ComicBubbleTextBox;
import eu.virtualparadox.comictoolset.translator.textmaskgenerator.TextMaskBox;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A utility class for merging comic bubble texts with their best matching text mask boxes.
 * <p>
 * Each {@link TextMaskBox} is assigned to the {@link ComicBubbleTextBox} it overlaps with the most.
 * Each mask box is used at most once. Bubbles without any overlapping masks remain unassigned.
 */
public final class ComicBubbleMaskBoxMerger {

    /**
     * Merges comic bubble texts with their best overlapping text mask boxes.
     *
     * @param comicBubbleTextBoxes the list of bubble texts (bounding boxes returned by OCR/LLM)
     * @param maskBoxes        the list of binary mask bounding boxes from segmentation
     * @return a list of {@link ComicBubbleTextMaskBox} instances pairing each bubble with its matching mask box (if any)
     */
    public List<ComicBubbleTextMaskBox> merge(final List<ComicBubbleTextBox> comicBubbleTextBoxes,
                                              final List<TextMaskBox> maskBoxes) {

        final List<ComicBubbleTextMaskBox> comicBubbleTextMaskBoxes = comicBubbleTextBoxes.stream()
                .map(ComicBubbleTextMaskBox::new)
                .toList();

        final Set<TextMaskBox> used = new HashSet<>();

        for (final TextMaskBox textMaskBox : maskBoxes) {
            if (used.contains(textMaskBox)) {
                continue;
            }

            final Optional<ComicBubbleTextMaskBox> maybeBestMatch = findBestMatch(comicBubbleTextMaskBoxes, textMaskBox);
            maybeBestMatch.ifPresent(bestMatch -> {
                bestMatch.addTextMaskBox(textMaskBox);
                used.add(textMaskBox);
            });
        }

        return comicBubbleTextMaskBoxes;
    }

    /**
     * Finds the {@link ComicBubbleTextMaskBox} whose {@link ComicBubbleTextBox} overlaps the most with the given mask.
     *
     * @param comicBubbleTextMaskBoxes   the list of union containers for each bubble
     * @param textMaskBox  the candidate mask to assign
     * @return the {@link ComicBubbleTextMaskBox} with the highest overlapping area, if any
     */
    private Optional<ComicBubbleTextMaskBox> findBestMatch(final List<ComicBubbleTextMaskBox> comicBubbleTextMaskBoxes,
                                                           final TextMaskBox textMaskBox) {
        ComicBubbleTextMaskBox bestMatch = null;
        int maxOverlap = 0;

        for (final ComicBubbleTextMaskBox comicBubbleTextMaskBox : comicBubbleTextMaskBoxes) {
            final int overlap = overlapArea(comicBubbleTextMaskBox.comicBubbleTextBox, textMaskBox);
            if (overlap > maxOverlap) {
                maxOverlap = overlap;
                bestMatch = comicBubbleTextMaskBox;
            }
        }

        return Optional.ofNullable(bestMatch);
    }

    /**
     * Computes the overlapping area between a bubble and a text mask box.
     *
     * @param bubble the comic bubble bounding box
     * @param box    the mask bounding box
     * @return the area of overlap (0 if none)
     */
    private int overlapArea(final ComicBubbleTextBox bubble,
                            final TextMaskBox box) {
        final int x1 = Math.max(bubble.x1(), box.x1);
        final int y1 = Math.max(bubble.y1(), box.y1);
        final int x2 = Math.min(bubble.x2(), box.x2);
        final int y2 = Math.min(bubble.y2(), box.y2);

        final int width = x2 - x1;
        final int height = y2 - y1;

        if (width <= 0 || height <= 0) {
            return 0;
        }

        return width * height;
    }
}
