package eu.virtualparadox.comictoolset.translator.textextractor;

import eu.virtualparadox.comictoolset.translator.bubbledetector.ComicBubbleBox;
import eu.virtualparadox.comictoolset.translator.bubbledetector.ComicBubbleBoxMerger;
import eu.virtualparadox.comictoolset.translator.bubbledetector.OnnxModelRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TextExtractor {

    private final Logger logger = LoggerFactory.getLogger(TextExtractor.class);
    private final TextBoxExtractor textBoxExtractor;

    public TextExtractor() {
        this.textBoxExtractor = new TextBoxExtractor();
    }

    public List<TextBox> extractTexts(final Path imagePath) {
        final List<ComicBubbleBox> boxes = extractBubbleBoxes(imagePath);
        final List<TextBox> textBoxes = generateTextBoxes(imagePath, boxes);
        for (final TextBox textBox : textBoxes) {
            logger.debug("{}", textBox.text());
        }
        return textBoxes;
    }

    private List<TextBox> generateTextBoxes(final Path imagePath, final List<ComicBubbleBox> boxes) {
        final List<TextBox> textBoxes = new ArrayList<>();
        for (final ComicBubbleBox box : boxes) {
            final TextBox textBox = textBoxExtractor.extractTextBox(imagePath, box.x1(), box.y1(), box.x2(), box.y2());
            textBoxes.add(textBox);
        }
        textBoxes.removeIf(p -> p.text().trim().isEmpty());
        textBoxes.sort(Comparator.comparingInt(TextBox::y1).thenComparingInt(TextBox::x1));
        return textBoxes;
    }

    private List<ComicBubbleBox> extractBubbleBoxes(Path imagePath) {
        try (final OnnxModelRunner omr1 = setUpModel("models/comic-speech-bubble-detector.onnx", 1024);
             final OnnxModelRunner omr2 = setUpModel("models/model_dynamic.onnx", 640)) {

            // Extract boxes from both models
            final List<ComicBubbleBox> boxes1 = omr1.run(imagePath);
            final List<ComicBubbleBox> boxes2 = omr2.run(imagePath);

            // Merge the two lists of boxes
            final ComicBubbleBoxMerger merger = new ComicBubbleBoxMerger();
            final List<ComicBubbleBox> mergedBoxes = new ArrayList<>();
            mergedBoxes.addAll(merger.merge(boxes1, 0.9f));
            mergedBoxes.addAll(merger.merge(boxes2, 0.9f));

            // Merge the boxes again to remove duplicates
            return merger.merge(mergedBoxes, 0.9f);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
