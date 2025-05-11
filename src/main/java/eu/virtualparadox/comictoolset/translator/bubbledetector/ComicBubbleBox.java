package eu.virtualparadox.comictoolset.translator.bubbledetector;

public record ComicBubbleBox(float x1, float y1, float x2, float y2, float confidence, int classId) {
    public float width() {
        return x2 - x1;
    }

    public float height() {
        return y2 - y1;
    }
}
