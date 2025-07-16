package eu.virtualparadox.comictoolset.translator.textboxgenerator.assigner;

import eu.virtualparadox.comictoolset.translator.textboxgenerator.recognizer.RecognizedTextBox;
import eu.virtualparadox.comictoolset.translator.textboxgenerator.maskgenerator.TextMaskRegion;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Utility class responsible for assigning binary text mask regions to recognized comic bubble text boxes.
 * <p>
 * The purpose of this class is to establish a link between text recognition results (from OCR or LLM)
 * and text regions detected via image segmentation (e.g., for masking or inpainting). This is a key step
 * in comic translation pipelines, where the original text must be removed and replaced.
 * <p>
 * The assignment strategy is based on spatial overlap: each {@link TextMaskRegion} is matched to the
 * {@link RecognizedTextBox} with which it has the greatest overlapping area. Each mask region is assigned
 * to at most one bubble. Bubbles without any overlapping mask remain assigned but empty.
 */
public final class BubbleTextAssigner {

    /**
     * Assigns each text mask region to the most overlapping recognized bubble text box.
     *
     * @param recognizedTextBoxes the list of recognized comic bubble texts from OCR or LLM
     * @param maskBoxes                 the list of mask regions (e.g. from binary segmentation)
     * @return a list of {@link RecognizedTextWithMask} objects pairing each recognized text with its matching mask(s)
     */
    public List<RecognizedTextWithMask> assign(final List<RecognizedTextBox> recognizedTextBoxes,
                                               final List<TextMaskRegion> maskBoxes) {

        final List<RecognizedTextWithMask> recognizedTextWithMasks = recognizedTextBoxes.stream()
                .map(RecognizedTextWithMask::new)
                .toList();

        final Set<TextMaskRegion> used = new HashSet<>();

        for (final TextMaskRegion textMaskRegion : maskBoxes) {
            if (used.contains(textMaskRegion)) {
                continue;
            }

            final Optional<RecognizedTextWithMask> maybeBestMatch = findBestMatch(recognizedTextWithMasks, textMaskRegion);
            maybeBestMatch.ifPresent(bestMatch -> {
                bestMatch.addTextMaskBox(textMaskRegion);
                used.add(textMaskRegion);
            });
        }

        return recognizedTextWithMasks;
    }

    /**
     * Finds the {@link RecognizedTextWithMask} whose associated {@link RecognizedTextBox}
     * has the largest overlapping area with the given mask region.
     *
     * @param recognizedTextWithMasks list of current bubble-to-mask associations
     * @param textMaskRegion        the candidate text mask region
     * @return an {@link Optional} containing the best-matching assignment, or empty if no overlap
     */
    private Optional<RecognizedTextWithMask> findBestMatch(final List<RecognizedTextWithMask> recognizedTextWithMasks,
                                                           final TextMaskRegion textMaskRegion) {
        RecognizedTextWithMask bestMatch = null;
        int maxOverlap = 0;

        for (final RecognizedTextWithMask recognizedTextWithMask : recognizedTextWithMasks) {
            final int overlap = overlapArea(recognizedTextWithMask.recognizedTextBox, textMaskRegion);
            if (overlap > maxOverlap) {
                maxOverlap = overlap;
                bestMatch = recognizedTextWithMask;
            }
        }

        return Optional.ofNullable(bestMatch);
    }

    /**
     * Calculates the area of overlap (in pixels) between a recognized bubble and a mask region.
     *
     * @param bubble the recognized bubble text box
     * @param box    the text mask region
     * @return the area of their intersection, or 0 if they do not overlap
     */
    private int overlapArea(final RecognizedTextBox bubble,
                            final TextMaskRegion box) {
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
