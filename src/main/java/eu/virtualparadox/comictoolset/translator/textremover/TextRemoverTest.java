package eu.virtualparadox.comictoolset.translator.textremover;

import eu.virtualparadox.comictoolset.translator.bubblecollector.BubbleCollector;
import eu.virtualparadox.comictoolset.translator.bubblecollector.BubbleModel;
import eu.virtualparadox.comictoolset.translator.bubblecollector.ComicBubbleBox;
import eu.virtualparadox.comictoolset.translator.bubblecollector.OnnxBubbleCollector;
import eu.virtualparadox.comictoolset.translator.bubblecollector.merger.ComicBubbleBoxMerger;
import eu.virtualparadox.comictoolset.translator.merger.ComicBubbleMaskBoxMerger;
import eu.virtualparadox.comictoolset.translator.merger.ComicBubbleTextMaskBox;
import eu.virtualparadox.comictoolset.translator.textboxgenerator.qwen.QwenTextBoxGenerator;
import eu.virtualparadox.comictoolset.translator.textboxgenerator.ComicBubbleTextBox;
import eu.virtualparadox.comictoolset.translator.textboxgenerator.VisualLLMModel;
import eu.virtualparadox.comictoolset.translator.textmaskgenerator.OnnxTextMaskGenerator;
import eu.virtualparadox.comictoolset.translator.textmaskgenerator.TextMaskBox;
import eu.virtualparadox.comictoolset.translator.textmaskgenerator.TextMaskGenerator;
import eu.virtualparadox.comictoolset.translator.textmaskgenerator.TextMaskModel;
import eu.virtualparadox.comictoolset.translator.textwriter.TextWriter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TextRemoverTest {

    public static void main(String[] args) throws Exception {
        // Example usage of TextRemover
        final Path originalImage = Path.of("src/test/resources/comics/transformers-01.png");

        final ComicBubbleBoxMerger comicBubbleBoxMerger = new ComicBubbleBoxMerger();
        final QwenTextBoxGenerator qwenTextBoxGenerator = new QwenTextBoxGenerator(VisualLLMModel.QWEN_25_7B, "http://localhost:11434/api/generate", true);
        final ComicBubbleMaskBoxMerger comicBubbleMaskBoxMerger = new ComicBubbleMaskBoxMerger();
        final TextWriter textWriter = new TextWriter();

        try (final BubbleCollector bubbleCollector = OnnxBubbleCollector.BubbleModelRunnerBuilder.builder().model(BubbleModel.COMIC_SPEECH_BUBBLE_DETECTOR).confidenceThreshold(0.01f).debug(true).build();
             final TextRemover textRemover = OnnxTextRemover.TextRemoverBuilder.builder().debug(true).model(TextRemoverModel.LAMA_FP32).build();
             final TextMaskGenerator textMaskGenerator = OnnxTextMaskGenerator.TextMaskModelRunnerBuilder.builder().debug(true).model(TextMaskModel.SMALL).paddingX(5).paddingY(5).build()) {

            final List<ComicBubbleBox> bubbleBoxes = bubbleCollector.extractBubbleBoxes(originalImage);
            final List<ComicBubbleBox> mergedBubbleBoxes = comicBubbleBoxMerger.merge(bubbleBoxes, 0.25f);

            final List<ComicBubbleTextBox> comicBubbleTextBoxes = qwenTextBoxGenerator.generateTextBoxes(originalImage, mergedBubbleBoxes);
            final List<TextMaskBox> maskBoxes = textMaskGenerator.getTextMask(originalImage);
            final List<TextMaskBox> filteredBoxes = boxesInsideBubble(mergedBubbleBoxes, maskBoxes);

            final Path resultImage = textRemover.removeText(originalImage, filteredBoxes);

            final List<ComicBubbleTextMaskBox> comicBubbleTextMaskBoxes = comicBubbleMaskBoxMerger.merge(comicBubbleTextBoxes, maskBoxes);
            textWriter.rewriteText(resultImage, resultImage.resolveSibling("clone.png"), comicBubbleTextMaskBoxes);

            System.out.println("Removed text: " + resultImage);
        }
    }

    private static List<TextMaskBox> boxesInsideBubble(final List<ComicBubbleBox> bubbleBoxes,
                                                       final List<TextMaskBox> maskBoxes) {
        final List<TextMaskBox> result = new ArrayList<>();
        for (final ComicBubbleBox bubbleBox : bubbleBoxes) {
            for (final TextMaskBox maskBox : maskBoxes) {
                if (bubbleBox.partialContains(maskBox.x1, maskBox.y1, maskBox.x2, maskBox.y2)) {
                    result.add(maskBox);
                }
            }
        }
        return result;
    }
}
