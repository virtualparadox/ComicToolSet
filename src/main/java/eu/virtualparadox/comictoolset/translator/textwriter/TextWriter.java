package eu.virtualparadox.comictoolset.translator.textwriter;

import eu.virtualparadox.comictoolset.translator.merger.ComicBubbleTextMaskBox;
import eu.virtualparadox.comictoolset.translator.textmaskgenerator.TextMaskBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TextWriter {

    private static final Logger logger = LoggerFactory.getLogger(TextWriter.class);

    public Path rewriteText(final Path path,
                            final Path outputPath,
                            final List<ComicBubbleTextMaskBox> boxes) throws IOException {
        final BufferedImage image = ImageIO.read(path.toFile());
        final Graphics2D g2d = image.createGraphics();

        for (final ComicBubbleTextMaskBox box : boxes) {
            final String translatedText = box.comicBubbleTextBox.text();

            final List<TextMaskBox> textBoxes = box.textMaskBox;
            drawTranslatedIntoBoxes(g2d, textBoxes, translatedText);
        }

        logger.info("Saving output image to: {}", outputPath);
        ImageIO.write(image, "png", outputPath.toFile());
        g2d.dispose();
        return outputPath;
    }

    private void drawTranslatedIntoBoxes(final Graphics2D g,
                                        final List<TextMaskBox> boxes,
                                        final String translatedText) {
        // 1. Sort boxes top-to-bottom
        boxes.sort(Comparator.comparingInt(p->p.y1));

        // 2. Split text into N lines
        int numLines = boxes.size();
        List<String> splitLines = splitTextIntoLines(translatedText, numLines);

        // 3. Render each line in its box
        for (int i = 0; i < boxes.size(); i++) {
            TextMaskBox box = boxes.get(i);
            String line = i < splitLines.size() ? splitLines.get(i) : "";
            drawTextInBox(g, box, line);
        }
    }

    private List<String> splitTextIntoLines(String text, int lines) {
        String[] words = text.split(" ");
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int targetWordsPerLine = Math.max(1, words.length / lines);

        int count = 0;
        for (String word : words) {
            if (count >= targetWordsPerLine && result.size() < lines - 1) {
                result.add(current.toString().trim());
                current = new StringBuilder();
                count = 0;
            }
            current.append(word).append(" ");
            count++;
        }
        if (!current.isEmpty()) {
            result.add(current.toString().trim());
        }

        // Padding in case there are more boxes than text lines
        while (result.size() < lines) result.add("");
        return result;
    }

    private void drawTextInBox(Graphics2D g, TextMaskBox box, String line) {
        int boxW = box.width();
        int boxH = box.height();

        for (int fontSize = 40; fontSize >= 8; fontSize--) {
            Font font = new Font("Arial", Font.PLAIN, fontSize);
            g.setFont(font);
            g.setColor(Color.RED);
            FontMetrics fm = g.getFontMetrics();
            int textW = fm.stringWidth(line);
            int textH = fm.getHeight();

            if (textW <= boxW && textH <= boxH) {
                int x = box.x1 + (boxW - textW) / 2;
                int y = box.y1 + (boxH - textH) / 2 + fm.getAscent();
                g.drawString(line, x, y);
                return;
            }
        }
    }


}
