package eu.virtualparadox.comictoolset.translator.textboxgenerator;

public record ComicBubbleTextBox(String text, String language, int x1, int y1, int x2, int y2) {
    public int width() {
        return x2 - x1;
    }

    public int height() {
        return y2 - y1;
    }
}
