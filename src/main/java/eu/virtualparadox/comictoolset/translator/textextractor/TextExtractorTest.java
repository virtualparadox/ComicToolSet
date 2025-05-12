package eu.virtualparadox.comictoolset.translator.textextractor;

import java.nio.file.Path;
import java.util.List;

public class TextExtractorTest {

    public static void main(String[] args) {
        final Path path = Path.of("src/test/resources/comics/dylan-dog-01.png");
        final TextExtractor textExtractor = new TextExtractor();

        final List<TextBox> boxes = textExtractor.extractTexts(path);
    }

}
