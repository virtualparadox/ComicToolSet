package eu.virtualparadox.comictoolset.translator.textmaskgenerator;

import ai.onnxruntime.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

import static eu.virtualparadox.comictoolset.ModelExtractor.extractModelToTempFile;

/**
 * A utility class for extracting speech/text regions from comic images using the PaddleOCR detection model (DBNet).
 * <p>
 * This class provides end-to-end support for loading an ONNX model from resources, preprocessing the input image,
 * running inference, post-processing the output heatmap, and extracting bounding boxes where text is detected.
 * </p>
 */
public class OnnxTextMaskGenerator implements TextMaskGenerator {

    /**
     * The ONNX Runtime environment for executing the model.
     */
    private final OrtEnvironment environment;

    /**
     * The ONNX Runtime session for running the model.
     */
    private final OrtSession session;

    /**
     * Padding values for the x and y axes to enlarge the detected text boxes.
     */
    private final int paddingX;
    private final int paddingY;

    /**
     * Flag to enable or disable debug mode.
     */
    private final boolean debug;

    /**
     * Debugging utility for visualizing the text mask generation process.
     */
    private final TextMaskDebugger debugger;

    /**
     * Constructor with extracted model path.
     *
     * @param env      ORT environment
     * @param session  ORT session
     * @param paddingX text box padding for the x-axis
     * @param paddingY text box padding for the y-axis
     * @param debug    need generate debug images
     */
    public OnnxTextMaskGenerator(final OrtEnvironment env,
                                 final OrtSession session,
                                 final int paddingX,
                                 final int paddingY,
                                 final boolean debug) {
        this.environment = env;
        this.session = session;
        this.paddingX = paddingX;
        this.paddingY = paddingY;
        this.debug = debug;
        this.debugger = new TextMaskDebugger();
    }

    /**
     * Performs inference on the given image to detect potential text regions.
     *
     * @param imagePath path to the comic image
     * @return list of detected bounding boxes with confidence values
     * @throws Exception if image loading or ONNX inference fails
     */
    public List<TextMaskBox> getTextMask(final Path imagePath) throws Exception {
        final BufferedImage originalImage = ImageIO.read(imagePath.toFile());
        final Dimension paddedSize = padToDivisible(originalImage.getWidth(), originalImage.getHeight(), 32);
        final BufferedImage resizedImage = resizeImage(originalImage, paddedSize.width, paddedSize.height);

        final FloatBuffer inputBuffer = preprocessImage(resizedImage);
        final long[] inputShape = {1, 3, paddedSize.height, paddedSize.width};

        final OnnxTensor inputTensor = OnnxTensor.createTensor(environment, inputBuffer, inputShape);
        final OrtSession.Result result = session.run(Collections.singletonMap("x", inputTensor));

        final float[][][][] output = (float[][][][]) result.get(0).getValue();
        final float[][] heatmap = output[0][0];
        final float[][] upscaledHeatmap = resizeHeatmap(heatmap, originalImage.getHeight(), originalImage.getWidth());
        final List<TextMaskBox> textBoxes = extractTextBoxes(upscaledHeatmap, 0.01f);

        if (debug) {
            debugger.saveDebugImage(imagePath, textBoxes, Color.RED);
        }

        return textBoxes;
    }

    /**
     * Pads the image size to the next multiple of the given divisor.
     * Required for ONNX DBNet model compatibility.
     */
    private Dimension padToDivisible(final int width, final int height, final int divisor) {
        final int newW = ((width + divisor - 1) / divisor) * divisor;
        final int newH = ((height + divisor - 1) / divisor) * divisor;
        return new Dimension(newW, newH);
    }

    /**
     * Smoothly resizes the image to the specified dimensions.
     */
    private BufferedImage resizeImage(final BufferedImage image, final int width, final int height) {
        final Image tmp = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        final BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        final Graphics2D g = resized.createGraphics();
        g.drawImage(tmp, 0, 0, null);
        g.dispose();
        return resized;
    }

    /**
     * Prepares a FloatBuffer in NCHW format by normalizing RGB values.
     * The normalization uses ImageNet mean and std.
     */
    private FloatBuffer preprocessImage(final BufferedImage image) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        final FloatBuffer buffer = FloatBuffer.allocate(1 * 3 * height * width);

        final float[] mean = {0.485f, 0.456f, 0.406f};
        final float[] std = {0.229f, 0.224f, 0.225f};

        for (int c = 0; c < 3; c++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    final int rgb = image.getRGB(x, y);
                    final int val = switch (c) {
                        case 0 -> (rgb >> 16) & 0xFF; // Red
                        case 1 -> (rgb >> 8) & 0xFF;  // Green
                        default -> rgb & 0xFF;        // Blue
                    };
                    final float normalized = (val / 255.0f - mean[c]) / std[c];
                    buffer.put(normalized);
                }
            }
        }
        buffer.rewind();
        return buffer;
    }

    /**
     * Resizes the raw heatmap back to the original image resolution using bilinear interpolation.
     */
    private float[][] resizeHeatmap(final float[][] src, final int targetH, final int targetW) {
        final float[][] dst = new float[targetH][targetW];
        final int srcH = src.length;
        final int srcW = src[0].length;

        for (int y = 0; y < targetH; y++) {
            for (int x = 0; x < targetW; x++) {
                final float srcX = x * (srcW - 1f) / (targetW - 1f);
                final float srcY = y * (srcH - 1f) / (targetH - 1f);

                final int x0 = (int) Math.floor(srcX), x1 = Math.min(x0 + 1, srcW - 1);
                final int y0 = (int) Math.floor(srcY), y1 = Math.min(y0 + 1, srcH - 1);
                final float dx = srcX - x0, dy = srcY - y0;

                final float top = src[y0][x0] * (1 - dx) + src[y0][x1] * dx;
                final float bot = src[y1][x0] * (1 - dx) + src[y1][x1] * dx;
                dst[y][x] = top * (1 - dy) + bot * dy;
            }
        }

        return dst;
    }

    /**
     * Applies thresholding and flood-fill on the upscaled heatmap to extract bounding boxes.
     *
     * @param heatmap   2D float array output from the ONNX model
     * @param threshold minimum activation level for a pixel to be considered text
     * @return list of {@link TextMaskBox} with bounding coordinates and confidence score
     */
    private List<TextMaskBox> extractTextBoxes(final float[][] heatmap,
                                               final float threshold) {
        final int h = heatmap.length;
        final int w = heatmap[0].length;
        final boolean[][] visited = new boolean[h][w];
        final List<TextMaskBox> boxes = new ArrayList<>();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!visited[y][x] && heatmap[y][x] > threshold) {
                    final TextMaskBox box = floodFill(heatmap, visited, x, y, threshold);
                    if (box.width() > 5 && box.height() > 5) {
                        boxes.add(box);
                    }
                }
            }
        }

        // Enlarge boxes by padding
        return boxes.stream()
                .map(p -> p.enlarge(paddingX, paddingY))
                .toList();
    }

    /**
     * Performs flood-fill from a seed pixel, expanding to all connected neighbors over threshold.
     * Accumulates the sum of heatmap activations to compute average confidence.
     *
     * @param heatmap   output activation map
     * @param visited   boolean mask tracking visited pixels
     * @param startX    seed x coordinate
     * @param startY    seed y coordinate
     * @param threshold pixel intensity threshold
     * @return bounding box of the region with average activation as confidence
     */
    private TextMaskBox floodFill(final float[][] heatmap,
                                  final boolean[][] visited,
                                  final int startX,
                                  final int startY,
                                  final float threshold) {
        final int h = heatmap.length;
        final int w = heatmap[0].length;

        int minX = startX;
        int minY = startY;
        int maxX = startX;
        int maxY = startY;
        float sum = heatmap[startY][startX];
        int count = 1;

        final Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(startX, startY));
        visited[startY][startX] = true;

        while (!queue.isEmpty()) {
            final Point p = queue.poll();
            final int x = p.x;
            final int y = p.y;

            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);

            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    final int nx = x + dx;
                    final int ny = y + dy;
                    if (nx >= 0 && ny >= 0 && nx < w && ny < h && !visited[ny][nx] && heatmap[ny][nx] > threshold) {
                        visited[ny][nx] = true;
                        queue.add(new Point(nx, ny));
                        sum += heatmap[ny][nx];
                        count++;
                    }
                }
            }
        }

        final float confidence = sum / count;
        return new TextMaskBox(minX, minY, maxX, maxY, confidence);
    }

    @Override
    public void close() throws Exception {
        session.close();
        environment.close();
    }

    /**
     * Builder customization for extracting the ONNX model from resources to a temporary file.
     */
    public static class TextMaskModelRunnerBuilder {
        private String modelPath;
        private boolean debug;
        private int paddingX;
        private int paddingY;

        public static TextMaskModelRunnerBuilder builder() {
            return new TextMaskModelRunnerBuilder();
        }

        public TextMaskModelRunnerBuilder model(TextMaskModel model) {
            this.modelPath = model.modelPath;
            return this;
        }

        public TextMaskModelRunnerBuilder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public TextMaskModelRunnerBuilder paddingX(int paddingX) {
            this.paddingX = paddingX;
            return this;
        }

        public TextMaskModelRunnerBuilder paddingY(int paddingY) {
            this.paddingY = paddingY;
            return this;
        }

        /**
         * Builds the {@link OnnxTextMaskGenerator} by extracting the model from resources into a temporary file.
         *
         * @return an initialized runner instance with a usable model path
         */
        public OnnxTextMaskGenerator build() throws Exception {
            if (modelPath == null) {
                throw new IllegalArgumentException("TextMaskModel must not be null");
            }
            final String extracted = extractModelToTempFile(modelPath);

            final OrtEnvironment env = OrtEnvironment.getEnvironment();
            final OrtSession session = env.createSession(extracted, new OrtSession.SessionOptions());

            return new OnnxTextMaskGenerator(env, session, paddingX, paddingY, debug);
        }
    }

}
