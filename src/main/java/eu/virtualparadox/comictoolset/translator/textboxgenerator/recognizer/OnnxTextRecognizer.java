package eu.virtualparadox.comictoolset.translator.textboxgenerator.recognizer;

import ai.onnxruntime.*;
import eu.virtualparadox.comictoolset.translator.bubblecollector.DetectedBubbleBox;
import eu.virtualparadox.comictoolset.translator.textboxgenerator.assigner.BubbleTextAssigner;
import eu.virtualparadox.comictoolset.translator.textboxgenerator.assigner.RecognizedTextWithMask;
import eu.virtualparadox.comictoolset.translator.textboxgenerator.maskgenerator.*;
import eu.virtualparadox.comictoolset.translator.textboxgenerator.processor.TextProcessor;
import smile.clustering.DBSCAN;
import smile.math.distance.EuclideanDistance;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

import static eu.virtualparadox.comictoolset.ModelExtractor.extractModelToTempFile;

/**
 * Text recognizer using ONNX-based PaddleOCR model.
 * Recognizes text from comic bubble images and associates text with detected mask regions.
 */
public final class OnnxTextRecognizer implements TextRecognizer {

    private final OrtEnvironment env;
    private final OrtSession session;
    private final List<String> labelList;
    private final TextMaskGenerator textMaskGenerator;
    private final TextProcessor textProcessor;

    private OnnxTextRecognizer(final TextMaskGenerator textMaskGenerator) throws Exception {
        this.env = OrtEnvironment.getEnvironment();
        final String modelPath = extractModelToTempFile("models/paddle/inference.onnx");
        this.session = env.createSession(modelPath, new OrtSession.SessionOptions());
        this.labelList = OcrDecoder.loadLabelList("models/paddle/en_dict.txt");
        this.textMaskGenerator = textMaskGenerator;
        this.textProcessor = new TextProcessor();
    }

    /**
     * Builds an instance using a small ONNX-based mask model with default padding.
     */
    public static OnnxTextRecognizer build() throws Exception {
        final TextMaskGenerator generator = OnnxTextMaskGenerator.TextMaskModelRunnerBuilder.builder()
                .model(TextMaskModel.SMALL)
                .paddingX(15)
                .paddingY(15)
                .build();
        return new OnnxTextRecognizer(generator);
    }

    /**
     * Recognizes text inside comic bubble regions.
     * Performs clustering, recognition, and assignment of recognized text to detected masks.
     */
    @Override
    public List<RecognizedTextWithMask> recognize(final Path imagePath,
                                                  final List<DetectedBubbleBox> mergedBoxes) throws Exception {
        final BufferedImage image = ImageIO.read(imagePath.toFile());
        final List<TextMaskRegion> textMaskRegions = textMaskGenerator.getTextMask(imagePath);

        final Map<DetectedBubbleBox, List<TextMaskRegion>> boxesByBubble = new HashMap<>();
        for (final TextMaskRegion box : textMaskRegions) {
            for (final DetectedBubbleBox bubble : mergedBoxes) {
                if (isInside(bubble, box)) {
                    boxesByBubble.computeIfAbsent(bubble, k -> new ArrayList<>()).add(box);
                    break;
                }
            }
        }

        final List<RecognizedTextBox> recognizedWords = new ArrayList<>();
        for (final TextMaskRegion box : textMaskRegions) {
            final BufferedImage crop = image.getSubimage(box.x1, box.y1, box.width(), box.height());
            recognizedWords.addAll(recognizeSingleBubble(crop, box.x1, box.y1));
        }

        final List<RecognizedTextBox> recognizedTextBoxes = mergeTextBoxes(mergedBoxes, recognizedWords);
        final BubbleTextAssigner assigner = new BubbleTextAssigner();
        return assigner.assign(recognizedTextBoxes, textMaskRegions);
    }

    private boolean isInside(final DetectedBubbleBox box, final TextMaskRegion word) {
        return word.x1 >= box.x1() && word.x2 <= box.x2()
                && word.y1 >= box.y1() && word.y2 <= box.y2();
    }

    private List<RecognizedTextBox> recognizeSingleBubble(final BufferedImage bubbleImage,
                                                          final int offsetX,
                                                          final int offsetY) throws Exception {
        final BufferedImage resized = ImageUtils.resizePreservingRatio(bubbleImage, 48);
        final FloatBuffer tensor = ImageUtils.toFloatTensor(resized, 3);
        final long[] shape = {1, 3, resized.getHeight(), resized.getWidth()};

        try (final OnnxTensor inputTensor = OnnxTensor.createTensor(env, tensor, shape)) {
            final OrtSession.Result result = session.run(Collections.singletonMap("x", inputTensor));
            final float[][][] logits = (float[][][]) result.get(0).getValue();
            final String decoded = OcrDecoder.ctcDecode(logits[0], labelList);
            return List.of(new RecognizedTextBox(decoded, "", offsetX, offsetY,
                    offsetX + bubbleImage.getWidth(), offsetY + bubbleImage.getHeight()));
        }
    }

    private List<RecognizedTextBox> mergeTextBoxes(final List<DetectedBubbleBox> bubbleBoxes,
                                                   final List<RecognizedTextBox> recognizedWords) {
        final Map<DetectedBubbleBox, List<RecognizedTextBox>> groupedWords = new HashMap<>();
        for (final RecognizedTextBox word : recognizedWords) {
            findBestFit(bubbleBoxes, word).ifPresent(
                    box -> groupedWords.computeIfAbsent(box, k -> new ArrayList<>()).add(word));
        }

        final List<RecognizedTextBox> result = new ArrayList<>();
        for (final Map.Entry<DetectedBubbleBox, List<RecognizedTextBox>> entry : groupedWords.entrySet()) {
            final DetectedBubbleBox box = entry.getKey();
            final List<RecognizedTextBox> words = entry.getValue();
            words.sort(Comparator.comparingInt(RecognizedTextBox::y1).thenComparingInt(RecognizedTextBox::x1));
            final String mergedText = mergeText(words);
            result.add(new RecognizedTextBox(mergedText, "", box.x1(), box.y1(), box.x2(), box.y2()));
        }

        return result;
    }

    private Optional<DetectedBubbleBox> findBestFit(final List<DetectedBubbleBox> bubbleBoxes,
                                                    final RecognizedTextBox word) {
        DetectedBubbleBox bestFit = null;
        double bestOverlap = 0;
        for (final DetectedBubbleBox box : bubbleBoxes) {
            final double overlap = calculateOverlap(box, word);
            if (overlap > bestOverlap) {
                bestOverlap = overlap;
                bestFit = box;
            }
        }
        return Optional.ofNullable(bestFit);
    }

    private double calculateOverlap(final DetectedBubbleBox box, final RecognizedTextBox word) {
        final int x1 = Math.max(box.x1(), word.x1());
        final int y1 = Math.max(box.y1(), word.y1());
        final int x2 = Math.min(box.x2(), word.x2());
        final int y2 = Math.min(box.y2(), word.y2());

        final int overlapWidth = Math.max(0, x2 - x1);
        final int overlapHeight = Math.max(0, y2 - y1);
        final double overlapArea = overlapWidth * overlapHeight;
        final double boxArea = (box.x2() - box.x1()) * (box.y2() - box.y1());
        return overlapArea / boxArea;
    }

    private String mergeText(final List<RecognizedTextBox> words) {
        final StringBuilder result = new StringBuilder();
        for (final RecognizedTextBox word : words) {
            result.append(word.text()).append(" ");
        }
        return textProcessor.processText(result.toString());
    }

    /**
     * Releases ONNX session and environment resources.
     */
    @Override
    public void close() throws Exception {
        session.close();
        env.close();
    }
}
