package eu.virtualparadox.comictoolset.translator.textboxgenerator.maskgenerator;

import java.util.Objects;

/**
 * Represents a detected text region as a bounding box with confidence.
 * Coordinates are top-left (x1, y1) and bottom-right (x2, y2).
 * If it's an enlarged box, original coordinates are also stored.
 */
public final class TextMaskRegion {
    public final int x1;
    public final int y1;
    public final int x2;
    public final int y2;

    public final int originalX1;
    public final int originalY1;
    public final int originalX2;
    public final int originalY2;

    public final float confidence;
    public final boolean enlarged;

    /**
     * @param x1         top-left x coordinate
     * @param y1         top-left y coordinate
     * @param x2         bottom-right x coordinate
     * @param y2         bottom-right y coordinate
     * @param confidence confidence score (placeholder: always 1.0 for now)
     */
    public TextMaskRegion(int x1, int y1, int x2, int y2, float confidence) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.originalX1 = x1;
        this.originalY1 = y1;
        this.originalX2 = x2;
        this.originalY2 = y2;
        this.confidence = confidence;
        this.enlarged = false;
    }

    private TextMaskRegion(int x1, int y1, int x2, int y2, int originalX1, int originalY1, int originalX2, int originalY2, float confidence) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.originalX1 = originalX1;
        this.originalY1 = originalY1;
        this.originalX2 = originalX2;
        this.originalY2 = originalY2;
        this.confidence = confidence;
        this.enlarged = true;
    }

    public int width() {
        return x2 - x1;
    }

    public int height() {
        return y2 - y1;
    }

    public TextMaskRegion enlarge(int paddingX, int paddingY) {
        return new TextMaskRegion(x1 - paddingX, y1 - paddingY, x2 + paddingX, y2 + paddingY, x1, y1, x2, y2, confidence);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TextMaskRegion that = (TextMaskRegion) o;
        return x1 == that.x1 && y1 == that.y1 && x2 == that.x2 && y2 == that.y2 && originalX1 == that.originalX1 && originalY1 == that.originalY1 && originalX2 == that.originalX2 && originalY2 == that.originalY2 && Float.compare(confidence, that.confidence) == 0 && enlarged == that.enlarged;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x1, y1, x2, y2, originalX1, originalY1, originalX2, originalY2, confidence, enlarged);
    }

    @Override
    public String toString() {
        return "TextMaskBox[" +
                "x1=" + x1 + ", " +
                "y1=" + y1 + ", " +
                "x2=" + x2 + ", " +
                "y2=" + y2 + ", " +
                "confidence=" + confidence + ']';
    }

}