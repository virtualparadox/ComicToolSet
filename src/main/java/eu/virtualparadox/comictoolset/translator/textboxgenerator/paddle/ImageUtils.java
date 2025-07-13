package eu.virtualparadox.comictoolset.translator.textboxgenerator.paddle;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.FloatBuffer;

/**
 * Utility class for image preprocessing tasks needed for ONNX-based OCR pipelines.
 */
public class ImageUtils {

    /**
     * Resizes the image to the specified target height while preserving the aspect ratio.
     * The resulting width is scaled proportionally to maintain the original aspect ratio.
     *
     * @param input        the input image to resize
     * @param targetHeight the desired height of the output image
     * @return a resized image with height = targetHeight and width adjusted proportionally
     */
    public static BufferedImage resizePreservingRatio(final BufferedImage input, final int targetHeight) {
        final int originalWidth = input.getWidth();
        final int originalHeight = input.getHeight();

        final float scale = (float) targetHeight / originalHeight;
        final int targetWidth = Math.round(originalWidth * scale);

        final BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(input, 0, 0, targetWidth, targetHeight, null);
        g.dispose();

        return resized;
    }

    /**
     * Converts a BufferedImage to a FloatBuffer in CHW format (Channel, Height, Width), normalized to [0,1].
     * This is the format expected by PaddleOCR ONNX models.
     *
     * @param image the input image (must be RGB)
     * @param channels number of channels to extract (3 for RGB)
     * @return FloatBuffer containing normalized pixel data in CHW layout
     */
    public static FloatBuffer toFloatTensor(final BufferedImage image, final int channels) {
        final int height = image.getHeight();
        final int width = image.getWidth();
        final FloatBuffer buffer = FloatBuffer.allocate(channels * height * width);

        for (int c = 0; c < channels; c++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    final int rgb = image.getRGB(x, y);
                    final int value = switch (c) {
                        case 0 -> (rgb >> 16) & 0xFF; // Red
                        case 1 -> (rgb >> 8) & 0xFF;  // Green
                        case 2 -> rgb & 0xFF;         // Blue
                        default -> 0;
                    };
                    buffer.put(value / 255.0f);
                }
            }
        }

        buffer.rewind();
        return buffer;
    }
}
