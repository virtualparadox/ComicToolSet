package eu.virtualparadox.comictoolset.translator.textwriter;

import eu.virtualparadox.comictoolset.translator.textboxgenerator.assigner.RecognizedTextWithMask;
import eu.virtualparadox.comictoolset.translator.textboxgenerator.maskgenerator.TextMaskRegion;
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

/**
 * Responsible for rewriting translated text into designated comic bubble regions.
 * <p>
 * This class takes recognized bubble regions (with mask bounding boxes) and inserts the translated text into them.
 * The algorithm dynamically determines the maximum font size that allows the entire text to fit across the
 * given mask boxes. It ensures that words are not split between boxes or lines and that the result is visually clean.
 */
public class TextWriter {

    private static final Logger logger = LoggerFactory.getLogger(TextWriter.class);

    /**
     * Rewrites the translated text into the image at the given path and writes the result to outputPath.
     *
     * @param path        input image path
     * @param outputPath  destination image path
     * @param boxes       list of recognized text boxes (each with translated text and target mask regions)
     * @return path to the rewritten image
     * @throws IOException if image loading or saving fails
     */
    public Path rewriteText(final Path path,
                            final Path outputPath,
                            final List<RecognizedTextWithMask> boxes) throws IOException {
        final BufferedImage image = ImageIO.read(path.toFile());
        final Graphics2D g2d = image.createGraphics();

        for (final RecognizedTextWithMask box : boxes) {
            final String translatedText = box.recognizedTextBox.text();
            final List<TextMaskRegion> textBoxes = box.textMaskRegions;
            drawTranslatedIntoBoxes(g2d, textBoxes, translatedText);
        }

        logger.info("Saving output image to: {}", outputPath);
        ImageIO.write(image, "png", outputPath.toFile());
        g2d.dispose();
        return outputPath;
    }

    /**
     * Attempts to write the translated text across the provided mask regions.
     * <p>
     * The algorithm splits the text into words, and then tries decreasing font sizes (from 40 to 8).
     * For each font size, it simulates line wrapping and box usage:
     * <ul>
     *     <li>It wraps words into lines within each box based on horizontal width limits.</li>
     *     <li>It then ensures the number of lines fits within the vertical space of each box.</li>
     *     <li>If the entire text fits at the current font size, it is rendered and the method exits.</li>
     *     <li>If not, the font size is decreased and retried.</li>
     * </ul>
     * If the smallest font size is reached and the text still doesn't fit, a warning is logged.
     *
     * @param g              the graphics context to draw with
     * @param boxes          sorted list of available text boxes
     * @param translatedText the full translated text to be written
     */
    private void drawTranslatedIntoBoxes(final Graphics2D g,
                                         final List<TextMaskRegion> boxes,
                                         final String translatedText) {
        boxes.sort(Comparator.comparingInt(p -> p.y1));

        final List<String> words = List.of(translatedText.split(" "));

        for (int fontSize = 40; fontSize >= 8; fontSize--) {
            final Font font = new Font("Arial", Font.PLAIN, fontSize);
            final FontMetrics fm = g.getFontMetrics(font);

            int wordIndex = 0;
            final List<List<String>> linesPerBox = new ArrayList<>();

            // Try to pack words into lines across boxes
            for (final TextMaskRegion box : boxes) {
                final List<String> lines = new ArrayList<>();
                final int lineHeight = fm.getHeight();
                final int maxLines = box.height() / lineHeight;

                StringBuilder currentLine = new StringBuilder();
                int linesUsed = 0;

                while (wordIndex < words.size() && linesUsed < maxLines) {
                    final String word = words.get(wordIndex);
                    final String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
                    final int textWidth = fm.stringWidth(testLine);

                    if (textWidth <= box.width()) {
                        currentLine = new StringBuilder(testLine);
                        wordIndex++;
                    } else {
                        lines.add(currentLine.toString());
                        currentLine = new StringBuilder();
                        linesUsed++;
                    }
                }

                if (!currentLine.isEmpty() && linesUsed < maxLines) {
                    lines.add(currentLine.toString());
                }

                linesPerBox.add(lines);

                if (wordIndex >= words.size()) {
                    break;
                }
            }

            // If all words were consumed, we have a fit at this font size
            if (wordIndex >= words.size()) {
                g.setFont(font);
                g.setColor(Color.RED);
                final FontMetrics finalFm = g.getFontMetrics();

                for (int i = 0; i < linesPerBox.size(); i++) {
                    drawTextLines(g, finalFm, boxes.get(i), linesPerBox.get(i));
                }
                return;
            }
        }

        logger.warn("Text did not fit into the provided boxes even at smallest font size");
    }

    /**
     * Draws a list of lines inside the given bounding box, vertically centered.
     * Each line is horizontally centered within the box.
     *
     * @param g     the graphics context
     * @param fm    the font metrics for measuring line height and width
     * @param box   the target text mask region
     * @param lines the lines to draw in this box
     */
    private void drawTextLines(final Graphics2D g,
                               final FontMetrics fm,
                               final TextMaskRegion box,
                               final List<String> lines) {
        final int lineHeight = fm.getHeight();
        final int totalHeight = lineHeight * lines.size();
        final int startY = box.y1 + (box.height() - totalHeight) / 2 + fm.getAscent();

        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            final int textWidth = fm.stringWidth(line);
            final int x = box.x1 + (box.width() - textWidth) / 2;
            final int y = startY + i * lineHeight;
            g.drawString(line, x, y);
        }
    }
}
