package eu.virtualparadox.comictoolset.translator.textboxgenerator.qwen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.virtualparadox.comictoolset.translator.bubblecollector.ComicBubbleBox;
import eu.virtualparadox.comictoolset.translator.ollamaclient.LocalOllamaClient;
import eu.virtualparadox.comictoolset.translator.textboxgenerator.ComicBubbleTextBox;
import eu.virtualparadox.comictoolset.translator.textboxgenerator.TextBoxGenerator;
import eu.virtualparadox.comictoolset.translator.textboxgenerator.VisualLLMModel;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class QwenTextBoxGenerator implements TextBoxGenerator {


    public static final String START_TAG = "```json";
    public static final String END_TAG = "```";

    private final String modelName;
    private final String apiUrl;

    private final ObjectMapper objectMapper;
    private final boolean debug;
    private final QwenTextBoxGeneratorDebugger debugger;

    public QwenTextBoxGenerator(final VisualLLMModel model,
                                final String apiUrl,
                                final boolean debug) {
        this.modelName = model.modelName;
        this.apiUrl = apiUrl;
        this.objectMapper = new ObjectMapper();
        this.debug = debug;
        this.debugger = new QwenTextBoxGeneratorDebugger();
    }

    @Override
    public List<ComicBubbleTextBox> generateTextBoxes(final Path imagePath,
                                                      final List<ComicBubbleBox> bubbleBoxes) throws IOException {
        final List<ComicBubbleTextBox> textBoxes = bubbleBoxes.stream()
                .map(bubbleBox -> generateTextBoxes(imagePath, bubbleBox.x1(), bubbleBox.y1(), bubbleBox.x2(), bubbleBox.y2()))
                .filter(p -> !p.isEmpty())
                .flatMap(List::stream)
                .toList();

        if (debug) {
            debugger.saveDebugImage(imagePath, textBoxes, Color.RED);
        }

        return textBoxes;
    }

    private List<ComicBubbleTextBox> generateTextBoxes(final Path imagePath,
                                                       final int x1, final int y1,
                                                       final int x2, final int y2) {
        try {
            final LocalOllamaClient client = LocalOllamaClient.builder()
                    .url(apiUrl)
                    .model(modelName)
                    .build();

            final Optional<String> maybeBase64Bubble = extractBase64Bubble(imagePath, x1, y1, x2, y2);
            if (maybeBase64Bubble.isEmpty()) {
                return Collections.emptyList();
            }

            final String result = client.extract(maybeBase64Bubble.get());
            final String between = StringUtils.substringBetween(result, START_TAG, END_TAG);
            if (between == null) {
                return Collections.emptyList();
            }

            return extract(between, x1, y1, x2, y2);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to run model", e);
        }
    }

    private List<ComicBubbleTextBox> extract(final String between,
                                             final int x1, final int y1,
                                             final int x2, final int y2) throws JsonProcessingException {
        final List<ExtractedText> texts = objectMapper.readValue(between, new TypeReference<>() {
        });
        final List<ComicBubbleTextBox> result = new ArrayList<>();
        for (ExtractedText text : texts) {
            final String language = text.language();
            final String textContent = text.text();
            if (StringUtils.isNotBlank(textContent)) {
                result.add(new ComicBubbleTextBox(textContent, language, x1, y1, x2, y2));
            }
        }
        return result;
    }

    private Optional<String> extractBase64Bubble(final Path path,
                                                 final int x1, final int y1,
                                                 final int x2, final int y2) {
        try {
            final BufferedImage image = ImageIO.read(path.toFile());
            final int width = image.getWidth();
            final int height = image.getHeight();
            if (x1 < 0 || y1 < 0 || x2 > width || y2 > height) {
                return Optional.empty();
            }

            final BufferedImage croppedImage = image.getSubimage(x1, y1, x2 - x1, y2 - y1);
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(croppedImage, "png", baos);
                final byte[] imageBytes = baos.toByteArray();
                final String encoded = Base64.getEncoder().encodeToString(imageBytes);
                return Optional.ofNullable(encoded);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error extracting bubble content", e);
        }
    }

    @Override
    public void close() throws Exception {
        // No resources to close
    }

    private record ExtractedText(String language, String text) {
    }
}
