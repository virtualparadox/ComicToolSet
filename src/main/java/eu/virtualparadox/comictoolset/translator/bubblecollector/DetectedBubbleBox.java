package eu.virtualparadox.comictoolset.translator.bubblecollector;

public record DetectedBubbleBox(int x1, int y1, int x2, int y2, float confidence, int classId) {
    public int width() {
        return x2 - x1;
    }

    public int height() {
        return y2 - y1;
    }

    /**
     * Check if there is any overlap between the two rectangles
     *
     * @return
     */
    public boolean partialContains(final int oX1,
                                   final int oY1,
                                   final int oX2,
                                   final int oY2) {
        return this.x1 < oX2 &&
                this.x2 > oX1 &&
                this.y1 < oY2 &&
                this.y2 > oY1;
    }
}
