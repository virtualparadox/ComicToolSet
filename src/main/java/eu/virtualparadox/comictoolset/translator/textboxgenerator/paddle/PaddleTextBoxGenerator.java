package eu.virtualparadox.comictoolset.translator.textboxgenerator.paddle;

import ai.onnxruntime.*;
import eu.virtualparadox.comictoolset.translator.bubblecollector.ComicBubbleBox;
import eu.virtualparadox.comictoolset.translator.bubblecollector.merger.ComicBubbleBoxMerger;
import eu.virtualparadox.comictoolset.translator.textboxgenerator.ComicBubbleTextBox;
import eu.virtualparadox.comictoolset.translator.textboxgenerator.TextBoxGenerator;
import eu.virtualparadox.comictoolset.translator.textmaskgenerator.OnnxTextMaskGenerator;
import eu.virtualparadox.comictoolset.translator.textmaskgenerator.TextMaskBox;
import eu.virtualparadox.comictoolset.translator.textmaskgenerator.TextMaskModel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static eu.virtualparadox.comictoolset.ModelExtractor.extractModelToTempFile;

/**
 * A text box generator that uses PaddleOCR recognizer to extract text from comic bubble regions.
 */
public class PaddleTextBoxGenerator implements TextBoxGenerator {

    private final OrtEnvironment env;
    private final OrtSession session;
    private final List<String> labelList;

    public PaddleTextBoxGenerator() throws Exception {
        this.env = OrtEnvironment.getEnvironment();
        final String modelPath = extractModelToTempFile("models/paddle/inference.onnx");
        this.session = env.createSession(modelPath, new OrtSession.SessionOptions());
        this.labelList = OcrDecoder.loadLabelList("models/paddle/en_dict.txt");
    }

    @Override
    public List<ComicBubbleTextBox> generateTextBoxes(final Path imagePath,
                                                      final List<ComicBubbleBox> bubbleBoxes) throws Exception {
        final BufferedImage image = ImageIO.read(imagePath.toFile());

        // merge the boxes first
        final ComicBubbleBoxMerger merger = new ComicBubbleBoxMerger();
        final List<ComicBubbleBox> mergedBoxes = merger.merge(bubbleBoxes, 0.25f);

        // this implementation can process only one word at a time, so we need
        // to initialize the text mask generator to recognize words first.
        try (final OnnxTextMaskGenerator textMaskGenerator = OnnxTextMaskGenerator.TextMaskModelRunnerBuilder.builder().paddingX(15).paddingY(15).model(TextMaskModel.SMALL).build()) {
            final List<TextMaskBox> textMaskBoxes = textMaskGenerator.getTextMask(imagePath);

            final List<ComicBubbleTextBox> recognizedWords = new ArrayList<>();
            for (TextMaskBox box : textMaskBoxes) {
                final BufferedImage crop = image.getSubimage(box.x1, box.y1, box.width(), box.height());
                recognizedWords.addAll(recognizeSingleBubble(crop, box.x1, box.y1));
            }

            // merge words into a single text box
            return mergeTextBoxes(mergedBoxes, recognizedWords);
        }
    }

    private List<ComicBubbleTextBox> recognizeSingleBubble(final BufferedImage bubbleImage,
                                                           final int offsetX,
                                                           final int offsetY) throws Exception {
        final BufferedImage resized = ImageUtils.resizePreservingRatio(bubbleImage, 48);
        ImageIO.write(resized, "png", Paths.get("/tmp/output.png").toFile());

        final FloatBuffer tensor = ImageUtils.toFloatTensor(resized, 3);

        final long[] shape = {1, 3, resized.getHeight(), resized.getWidth()};
        final OnnxTensor inputTensor = OnnxTensor.createTensor(env, tensor, shape);

        final OrtSession.Result result = session.run(Collections.singletonMap("x", inputTensor));
        final float[][][] logits = (float[][][]) result.get(0).getValue();

        final String decoded = OcrDecoder.ctcDecode(logits[0], labelList);
        final ComicBubbleTextBox box = new ComicBubbleTextBox(decoded, "", offsetX, offsetY, offsetX + bubbleImage.getWidth(), offsetY + bubbleImage.getHeight());
        return List.of(box);
    }

    private List<ComicBubbleTextBox> mergeTextBoxes(final List<ComicBubbleBox> bubbleBoxes,
                                                    final List<ComicBubbleTextBox> recognizedWords) {
        final Map<ComicBubbleBox, List<ComicBubbleTextBox>> groupedWords = new HashMap<>();
        for (ComicBubbleTextBox word : recognizedWords) {
            final Optional<ComicBubbleBox> bestFit = findBestFit(bubbleBoxes, word);
            bestFit.ifPresent(comicBubbleBox -> groupedWords.computeIfAbsent(comicBubbleBox, k -> new ArrayList<>()).add(word));
        }

        final List<ComicBubbleTextBox> result = new ArrayList<>();
        for (Map.Entry<ComicBubbleBox, List<ComicBubbleTextBox>> entry : groupedWords.entrySet()) {
            final ComicBubbleBox box = entry.getKey();
            final List<ComicBubbleTextBox> words = entry.getValue();
            words.sort(Comparator.comparingInt(ComicBubbleTextBox::y1).thenComparingInt(ComicBubbleTextBox::x1));

            final String mergedText = mergeText(words);
            final ComicBubbleTextBox resultBox = new ComicBubbleTextBox(mergedText, "", box.x1(), box.y1(), box.x2(), box.y2());
            result.add(resultBox);
        }

        return result;
    }


    private Optional<ComicBubbleBox> findBestFit(final List<ComicBubbleBox> bubbleBoxes,
                                                 final ComicBubbleTextBox word) {
        ComicBubbleBox bestFit = null;
        double bestOverlap = 0;
        for (ComicBubbleBox box : bubbleBoxes) {
            final double overlap = calculateOverlap(box, word);
            if (overlap > bestOverlap) {
                bestOverlap = overlap;
                bestFit = box;
            }
        }
        return Optional.ofNullable(bestFit);
    }

    private double calculateOverlap(ComicBubbleBox box, ComicBubbleTextBox word) {
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

    private String mergeText(List<ComicBubbleTextBox> words) {
        final StringBuilder result = new StringBuilder();
        for (ComicBubbleTextBox word : words) {
            result.append(word.text()).append(" ");
        }
        return result.toString().trim();
    }

    @Override
    public void close() throws Exception {
        session.close();
        env.close();
    }
}
