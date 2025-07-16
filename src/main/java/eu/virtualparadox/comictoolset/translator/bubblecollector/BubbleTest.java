package eu.virtualparadox.comictoolset.translator.bubblecollector;

import java.nio.file.Path;

public class BubbleTest {
    public static void main(String[] args) {
        try (final OnnxBubbleCollector omr1 = setUpModel(BubbleModel.MODEL_DYNAMIC);
             final OnnxBubbleCollector omr2 = setUpModel(BubbleModel.COMIC_SPEECH_BUBBLE_DETECTOR)) {

            omr1.extractBubbleBoxes(Path.of("src/test/resources/comics/american-vampire-01.png"));
            omr2.extractBubbleBoxes(Path.of("src/test/resources/comics/american-vampire-01.png"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static OnnxBubbleCollector setUpModel(final BubbleModel model) {
        return OnnxBubbleCollector.BubbleModelRunnerBuilder.builder()
                .model(model)
                .confidenceThreshold(0.1f)
                .debug(true)
                .build();
    }
}
