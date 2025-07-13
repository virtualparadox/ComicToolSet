package eu.virtualparadox.comictoolset.translator.textmaskgenerator;

import java.nio.file.Path;
import java.util.List;

public class TextMaskTest {

    public static void main(String[] args) throws Exception {
        final Path imagePath = Path.of("src/test/resources/comics/dylan-dog-04.jpg");
        try (final TextMaskGenerator textMaskGenerator = setUpModel()) {
            final List<TextMaskBox> boxes = textMaskGenerator.getTextMask(imagePath);
            System.out.println("Found " + boxes.size() + " text mask boxes");
        }
    }

    private static OnnxTextMaskGenerator setUpModel() throws Exception {
        return OnnxTextMaskGenerator.TextMaskModelRunnerBuilder.builder()
                .model(TextMaskModel.SMALL)
                .debug(true)
                .build();
    }
}
