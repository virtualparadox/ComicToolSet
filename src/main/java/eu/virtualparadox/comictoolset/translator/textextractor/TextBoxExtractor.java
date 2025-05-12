package eu.virtualparadox.comictoolset.translator.textextractor;

import org.apache.commons.lang3.StringUtils;
import org.bytedeco.leptonica.PIX;
import org.bytedeco.leptonica.global.leptonica;
import org.bytedeco.tesseract.TessBaseAPI;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

public final class TextBoxExtractor {

    public TextBox extractTextBox(final Path imagePath,
                                  final int x1,
                                  final int y1,
                                  final int x2,
                                  final int y2) {

        try {
            // Load the image
            final BufferedImage original = ImageIO.read(imagePath.toFile());
            final int width = original.getWidth();
            final int height = original.getHeight();

            // Create RGB copy and mask outside box
            final BufferedImage masked = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            final Graphics2D g = masked.createGraphics();
            g.drawImage(original, 0, 0, null);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, y1);
            g.fillRect(0, y2 + 1, width, height - y2 - 1);
            g.fillRect(0, y1, x1, y2 - y1 + 1);
            g.fillRect(x2 + 1, y1, width - x2 - 1, y2 - y1 + 1);
            g.dispose();

            // Convert to grayscale and binarize
            final BufferedImage gray = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            Graphics gGray = gray.getGraphics();
            gGray.drawImage(masked, 0, 0, null);
            gGray.dispose();

            // Binarize the image
            final BufferedImage binary = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = gray.getRGB(x, y);
                    int grayVal = rgb & 0xFF;
                    int bin = (grayVal < 220) ? 0 : 0xFFFFFF;
                    binary.setRGB(x, y, bin);
                }
            }

            // Save to temp file
            final File temp = File.createTempFile("ocr-input", ".png");
            ImageIO.write(binary, "png", temp);
            temp.deleteOnExit();

            // Run Tesseract
            try (final TessBaseAPI api = new TessBaseAPI()) {

                if (api.Init("/opt/homebrew/share/tessdata", "ita") != 0) {
                    throw new IllegalStateException("Could not initialize Tesseract");
                }

                final PIX pix = leptonica.pixRead(temp.getAbsolutePath());
                api.SetImage(pix);

                final String text = cleanText(api.GetUTF8Text().getString());

                api.End();
                leptonica.pixDestroy(pix);

                return new TextBox(x1, y1, x2, y2, text);
            }

        } catch (final Exception e) {
            throw new RuntimeException("OCR failed", e);
        }
    }

    private String cleanText(String string) {
        // replace multiple spaces with a single space
        final String clean1 = string.replaceAll("\\s+", " ");
        // remove line breaks
        final String clean2 = clean1.replaceAll("[\\r\\n]+", " ");
        // remove all non-alphanumeric characters except for .,!? and whitespace
        final String clean3 = removeNonAlphanumeric(clean2);
        // trim
        return clean3.trim();
    }

    private String removeNonAlphanumeric(String string) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            final char c = string.charAt(i);
            if (Character.isLetterOrDigit(c) || StringUtils.contains(".,!?-\" ", c)) {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
