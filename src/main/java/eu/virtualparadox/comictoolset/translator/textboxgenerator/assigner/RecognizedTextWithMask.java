package eu.virtualparadox.comictoolset.translator.textboxgenerator.assigner;

import eu.virtualparadox.comictoolset.translator.textboxgenerator.recognizer.RecognizedTextBox;
import eu.virtualparadox.comictoolset.translator.textboxgenerator.maskgenerator.TextMaskRegion;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the assignment of one recognized comic bubble text box to one or more text mask regions.
 * <p>
 * In comic image translation workflows, OCR or LLM-based recognition yields {@link RecognizedTextBox}
 * objects representing the detected speech bubble texts. Separately, image segmentation may identify
 * {@link TextMaskRegion} objects indicating regions of text to be removed (e.g. for inpainting).
 * <p>
 * This class associates each recognized bubble text with the best-matching one or more text mask regions
 * based on spatial overlap, allowing further steps like text removal, translation, or inpainting to be
 * carried out precisely.
 */
public class RecognizedTextWithMask {

    /** The recognized text box from OCR/LLM inside a comic bubble. */
    public final RecognizedTextBox recognizedTextBox;

    /** The list of text mask regions associated with this bubble text box. */
    public final List<TextMaskRegion> textMaskRegions;

    /**
     * Constructs a new {@code BubbleTextAssignment} for the given recognized bubble text.
     *
     * @param recognizedTextBox the recognized bubble text box to be associated with mask regions
     */
    public RecognizedTextWithMask(final RecognizedTextBox recognizedTextBox) {
        this.recognizedTextBox = recognizedTextBox;
        this.textMaskRegions = new ArrayList<>();
    }

    /**
     * Adds a text mask region to the list associated with this bubble text.
     *
     * @param textMaskRegion the mask region to assign
     */
    public void addTextMaskBox(final TextMaskRegion textMaskRegion) {
        this.textMaskRegions.add(textMaskRegion);
    }
}
