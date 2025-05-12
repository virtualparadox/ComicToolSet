package eu.virtualparadox.comictoolset.translator.bubbledetector;

public record ComicBubbleBox(int x1, int y1, int x2, int y2, float confidence, int classId) {
    public int width() {
        return x2 - x1;
    }

    public int height() {
        return y2 - y1;
    }
}
