package eu.virtualparadox.comictoolset.translator.bubblecollector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class BubbleModelDebugger {

    private final Logger logger = LoggerFactory.getLogger(BubbleModelDebugger.class);

    /**
     * Saves a debug image to the system temp directory with visual bounding boxes.
     *
     * @param imagePath the original input image path
     * @param boxes     the list of bounding boxes
     * @param boxColor  the color of the bounding boxes
     * @throws Exception if writing fails
     */
    public void saveDebugImage(final Path imagePath,
                               final List<DetectedBubbleBox> boxes,
                               final Color boxColor) throws IOException {
        final BufferedImage originalImage = ImageIO.read(imagePath.toFile());

        final BufferedImage output = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = output.createGraphics();
        g.drawImage(originalImage, 0, 0, null);
        g.setFont(new Font("Arial", Font.BOLD, 14));

        for (final DetectedBubbleBox box : boxes) {
            g.setColor(boxColor);
            g.setStroke(new BasicStroke(3));
            g.drawRect(box.x1(), box.y1(), box.width(), box.height());
            g.setColor(Color.WHITE);
            g.drawString(String.format("ID: %d Conf: %.2f", box.classId(), box.confidence()), box.x1(), (box.y1() - 5));
        }

        g.dispose();

        final String baseName = stripExtension(imagePath.toFile().getName());
        final File debugFile = File.createTempFile("bubble-" + baseName + "-debug-", ".jpg");
        debugFile.deleteOnExit();
        ImageIO.write(output, "jpg", debugFile);
        logger.info("Debug image saved to temp: {}", debugFile.getAbsolutePath());
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
