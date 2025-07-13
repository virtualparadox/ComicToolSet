package eu.virtualparadox.comictoolset.translator.textboxgenerator.paddle;

import eu.virtualparadox.comictoolset.translator.bubblecollector.BubbleCollector;
import eu.virtualparadox.comictoolset.translator.bubblecollector.BubbleModel;
import eu.virtualparadox.comictoolset.translator.bubblecollector.ComicBubbleBox;
import eu.virtualparadox.comictoolset.translator.bubblecollector.OnnxBubbleCollector;
import eu.virtualparadox.comictoolset.translator.textboxgenerator.ComicBubbleTextBox;

import java.nio.file.Path;
import java.util.List;

public class PaddleTest {

    public static void main(final String[] args) throws Exception {
        final Path image = Path.of("src/test/resources/comics/dylan-dog-02.png");
        try (final BubbleCollector bubbleCollector = OnnxBubbleCollector.BubbleModelRunnerBuilder.builder().model(BubbleModel.COMIC_SPEECH_BUBBLE_DETECTOR).confidenceThreshold(0.01f).debug(true).build()) {
            final List<ComicBubbleBox> bubbleBoxes = bubbleCollector.extractBubbleBoxes(image);

            final PaddleTextBoxGenerator paddleTextBoxGenerator = new PaddleTextBoxGenerator();
            final List<ComicBubbleTextBox> lofasz = paddleTextBoxGenerator.generateTextBoxes(image, bubbleBoxes);
            System.out.println();
        }
    }
}
