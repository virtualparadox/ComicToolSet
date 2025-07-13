package eu.virtualparadox.comictoolset.translator.textboxgenerator.qwen;

import eu.virtualparadox.comictoolset.translator.textboxgenerator.ComicBubbleTextBox;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class QwenTextBoxGeneratorDebugger {

    /**
     * Saves a debug image to the system temp directory with visual bounding boxes.
     *
     * @param imagePath the original input image path
     * @param boxes     the list of bounding boxes
     * @param boxColor  the color of the bounding boxes
     * @throws Exception if writing fails
     */
    public void saveDebugImage(final Path imagePath,
                               final List<ComicBubbleTextBox> boxes,
                               final Color boxColor) throws IOException {
        final BufferedImage originalImage = ImageIO.read(imagePath.toFile());

        final BufferedImage output = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = output.createGraphics();
        g.drawImage(originalImage, 0, 0, null);

        for (final ComicBubbleTextBox box : boxes) {
            g.setColor(boxColor);
            g.setStroke(new BasicStroke(1));
            g.drawRect(box.x1(), box.y1(), box.width(), box.height());
        }

        g.dispose();

        final String baseName = stripExtension(imagePath.toFile().getName());
        final File debugFile = File.createTempFile("qwen-textbox-" + baseName + "-debug-", ".jpg");
        debugFile.deleteOnExit();
        ImageIO.write(output, "jpg", debugFile);
    }

    /**
     * Removes the file extension from a filename.
     *
     * @param filename the filename string
     * @return the name without extension
     */
    private String stripExtension(final String filename) {
        final int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? filename : filename.substring(0, dotIndex);
    }
}
