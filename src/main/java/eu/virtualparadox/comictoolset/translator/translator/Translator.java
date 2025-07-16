package eu.virtualparadox.comictoolset.translator.translator;

import eu.virtualparadox.comictoolset.translator.bubblecollector.BubbleCollector;
import eu.virtualparadox.comictoolset.translator.bubblecollector.BubbleModel;
import eu.virtualparadox.comictoolset.translator.bubblecollector.DetectedBubbleBox;
import eu.virtualparadox.comictoolset.translator.bubblecollector.OnnxBubbleCollector;
import eu.virtualparadox.comictoolset.translator.bubblecollector.merger.ComicBubbleBoxMerger;
import eu.virtualparadox.comictoolset.translator.textboxgenerator.assigner.RecognizedTextWithMask;
import eu.virtualparadox.comictoolset.translator.textboxgenerator.recognizer.TextRecognizer;
import eu.virtualparadox.comictoolset.translator.textboxgenerator.recognizer.OnnxTextRecognizer;
import eu.virtualparadox.comictoolset.translator.textboxgenerator.maskgenerator.TextMaskRegion;
import eu.virtualparadox.comictoolset.translator.textremover.OnnxTextRemover;
import eu.virtualparadox.comictoolset.translator.textremover.TextRemover;
import eu.virtualparadox.comictoolset.translator.textremover.TextRemoverModel;
import eu.virtualparadox.comictoolset.translator.textwriter.TextWriter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

public class Translator {

    private static final Logger logger = LoggerFactory.getLogger(Translator.class);

    private final String ollamaUrl;
    private final Path comicRootPath;
    private final Path outputPath;

    public Translator(final String ollamaUrl, final Path inputPath, final Path outputPath) {
        this.ollamaUrl = ollamaUrl;
        this.comicRootPath = inputPath;
        this.outputPath = outputPath;
    }

    public void translate() {
        // iterate on image files in the comicRootPath
        final File[] files = comicRootPath.toFile().listFiles();
        if (files == null) {
            System.err.println("No files found in the directory: " + comicRootPath);
            return;
        }

        final List<File> fileList = Arrays.asList(files);
        fileList.sort((o1, o2) -> StringUtils.compare(o1.getName(), o2.getName()));

        int n = 0;
        for (final File file : fileList) {
            final String outputFilename = StringUtils.leftPad(String.valueOf(n), 4, "0");
            final Path outputFilePath = outputPath.resolve(outputFilename + ".png");
            logger.info("Translating {} --> {}", file.getName(), outputFilePath);
            translate(file.toPath(), outputFilePath);
            n++;
        }
    }

    private void translate(final Path imagePath,
                           final Path outputPath) {
        final ComicBubbleBoxMerger merger = new ComicBubbleBoxMerger();
        final TextWriter textWriter = new TextWriter();

        try (final BubbleCollector bubbleCollector = setUpBubbleCollector();
             final TextRecognizer textRecognizer = setUpTextRecognizer();
             final TextRemover textRemover = setUpTextRemover()) {

            final List<DetectedBubbleBox> bubbles = bubbleCollector.extractBubbleBoxes(imagePath);
            final List<DetectedBubbleBox> mergedBubbles = merger.merge(bubbles, 0.9f);
            logger.info("{} bubble found...", mergedBubbles.size());

            final List<RecognizedTextWithMask> bubbleTextMaskBoxes = textRecognizer.recognize(imagePath, mergedBubbles);
            logger.info("{} text mask boxes found...", bubbleTextMaskBoxes.size());

            // TODO: translation logic here...

            final List<TextMaskRegion> textsToRemove = collectTextsToRemove(bubbleTextMaskBoxes);

            final Path cleanImage = textRemover.removeText(imagePath, textsToRemove);
            final Path rewritedImage = textWriter.rewriteText(cleanImage, outputPath, bubbleTextMaskBoxes);
            logger.info("Translated image saved to: {}", rewritedImage);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private TextRecognizer setUpTextRecognizer() {
        try {
            return OnnxTextRecognizer.build();
        } catch (Exception e) {
            logger.error("Failed to initialize PaddleTextBoxGenerator", e);
            throw new RuntimeException("Could not initialize text box generator", e);
        }
    }

    private List<TextMaskRegion> collectTextsToRemove(List<RecognizedTextWithMask> bubbleTextMaskBoxes) {
        final List<TextMaskRegion> result = new ArrayList<>();
        for (RecognizedTextWithMask comicBubbleTextBox : bubbleTextMaskBoxes) {
            result.addAll(comicBubbleTextBox.textMaskRegions);
        }
        return result;
    }

    private TextRemover setUpTextRemover() throws Exception {
        return OnnxTextRemover.TextRemoverBuilder.builder().model(TextRemoverModel.LAMA_FP32).build();
    }

    private OnnxBubbleCollector setUpBubbleCollector() {
        return OnnxBubbleCollector.BubbleModelRunnerBuilder.builder()
                .model(BubbleModel.COMIC_SPEECH_BUBBLE_DETECTOR)
                .confidenceThreshold(0.1f)
                .debug(false)
                .build();
    }
}
