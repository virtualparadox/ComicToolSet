package eu.virtualparadox.comictoolset.translator.textextractor;

import eu.virtualparadox.comictoolset.translator.bubbledetector.ComicBubbleBox;
import eu.virtualparadox.comictoolset.translator.bubbledetector.OnnxModelRunner;

import java.nio.file.Path;
import java.util.List;

public class TextExtractor {

    public List<TextBox> extractTexts(final Path imagePath) {
        final List<ComicBubbleBox> boxes = extractBubbleBoxes(imagePath);
        return List.of();
    }

    private List<ComicBubbleBox> extractBubbleBoxes(Path imagePath) {
        try (final OnnxModelRunner omr1 = setUpModel("models/comic-speech-bubble-detector.onnx", 1024);
             final OnnxModelRunner omr2 = setUpModel("models/model_dynamic.onnx", 640)) {

            omr1.run(Path.of("src/test/resources/comics/american-vampire-01.png"));
            omr2.run(Path.of("src/test/resources/comics/american-vampire-01.png"));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return List.of();
    }

    private OnnxModelRunner setUpModel(String modelFilename, int inputSize) {
        return OnnxModelRunner.builder()
                .modelFilename(modelFilename)
                .inputSize(inputSize)
                .confidenceThreshold(0.1f)
                .debug(true)
                .build();
    }
}
