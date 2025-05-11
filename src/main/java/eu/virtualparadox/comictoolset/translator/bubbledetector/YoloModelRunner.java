package eu.virtualparadox.comictoolset.translator.bubbledetector;

import ai.onnxruntime.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A YOLO-based ONNX model runner that detects comic speech bubbles in images.
 * Supports debug mode that generates a temporary annotated image with bounding boxes.
 */
public final class YoloModelRunner implements AutoCloseable {

    private final OrtEnvironment env;
    private final OrtSession session;
    private final int inputSize;
    private final float confidenceThreshold;
    private final boolean debug;
    private final ComicBubbleBoxDebugger debugger;

    /**
     * Constructs a new YOLO model runner.
     *
     * @param modelPath           path to the ONNX model file
     * @param inputSize           the expected square input size (e.g. 640 or 1024)
     * @param confidenceThreshold detections below this confidence will be filtered out
     * @param debug               whether to output a temporary annotated image with results
     * @throws Exception if model loading fails
     */
    public YoloModelRunner(final String modelPath,
                           final int inputSize,
                           final float confidenceThreshold,
                           final boolean debug) throws Exception {
        this.env = OrtEnvironment.getEnvironment();
        this.session = env.createSession(modelPath, new OrtSession.SessionOptions());
        this.inputSize = inputSize;
        this.confidenceThreshold = confidenceThreshold;
        this.debug = debug;
        this.debugger = new ComicBubbleBoxDebugger();
    }

    /**
     * Runs inference on the given image and returns the list of detected bubbles.
     *
     * @param imagePath path to the input image
     * @return list of {@link ComicBubbleBox} results
     * @throws Exception if preprocessing or inference fails
     */
    public List<ComicBubbleBox> run(final Path imagePath) throws Exception {
        final BufferedImage image = ImageIO.read(imagePath.toFile());
        final int origWidth = image.getWidth();
        final int origHeight = image.getHeight();

        final BufferedImage resizedImage = resizeImage(image, inputSize, inputSize);
        final float[] inputTensor = preprocessImage(resizedImage, inputSize);
        final long[] shape = {1, 3, inputSize, inputSize};
        final OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputTensor), shape);

        final OrtSession.Result result;
        synchronized (session) {
            final Map<String, OnnxTensor> inputMap =
                    Collections.singletonMap(session.getInputNames().iterator().next(), tensor);
            result = session.run(inputMap);
        }

        final Object outputObj = result.get(0).getValue();
        final float[][] detections;

        if (outputObj instanceof float[][][] output) {
            detections = (output.length == 1 && output[0][0].length == 6) ? output[0] : transpose(output[0]);
        } else {
            throw new RuntimeException("Unexpected output shape: " + outputObj.getClass());
        }

        final List<ComicBubbleBox> boxes = extractBoxes(detections, origWidth, origHeight);

        if (debug) {
            debugger.saveDebugImage(imagePath, boxes, Color.RED);
        }

        tensor.close();
        result.close();
        return boxes;
    }

    /**
     * Transposes a 2D float matrix.
     *
     * @param original the input matrix
     * @return the transposed matrix
     */
    private float[][] transpose(final float[][] original) {
        final float[][] transposed = new float[original[0].length][original.length];
        for (int i = 0; i < original.length; i++) {
            for (int j = 0; j < original[i].length; j++) {
                transposed[j][i] = original[i][j];
            }
        }
        return transposed;
    }

    /**
     * Resizes an image using bilinear interpolation.
     *
     * @param originalImage input image
     * @param width         target width
     * @param height        target height
     * @return resized image
     */
    private BufferedImage resizeImage(final BufferedImage originalImage,
                                      final int width,
                                      final int height) {
        final BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(originalImage, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }

    /**
     * Prepares image tensor in NCHW format normalized to [0, 1].
     *
     * @param image input image
     * @param size  expected input size
     * @return float tensor (NCHW)
     */
    private float[] preprocessImage(final BufferedImage image,
                                    final int size) {
        final float[] tensor = new float[3 * size * size];
        final int[] pixels = image.getRGB(0, 0, size, size, null, 0, size);
        for (int i = 0; i < size * size; i++) {
            final int rgb = pixels[i];
            tensor[i] = ((rgb >> 16) & 0xFF) / 255.0f;               // Red
            tensor[i + size * size] = ((rgb >> 8) & 0xFF) / 255.0f;  // Green
            tensor[i + 2 * size * size] = (rgb & 0xFF) / 255.0f;     // Blue
        }
        return tensor;
    }

    /**
     * Converts raw detection results into bounding boxes scaled to original image dimensions.
     *
     * @param detections model output (array of [cx, cy, w, h, conf, class])
     * @param origWidth  original image width
     * @param origHeight original image height
     * @return list of filtered {@link ComicBubbleBox} instances
     */
    private List<ComicBubbleBox> extractBoxes(final float[][] detections,
                                              final int origWidth,
                                              final int origHeight) {
        final List<ComicBubbleBox> boxes = new ArrayList<>();
        final float scaleX = origWidth / (float) inputSize;
        final float scaleY = origHeight / (float) inputSize;

        for (final float[] row : detections) {
            if (row.length < 6) continue;

            final float cx = row[0];
            final float cy = row[1];
            final float w = row[2];
            final float h = row[3];
            final float confidence = row[4];
            final int classId = (int) row[5];

            if (confidence < confidenceThreshold) continue;

            final float x1 = (cx - w / 2) * scaleX;
            final float y1 = (cy - h / 2) * scaleY;
            final float x2 = (cx + w / 2) * scaleX;
            final float y2 = (cy + h / 2) * scaleY;

            boxes.add(new ComicBubbleBox(x1, y1, x2, y2, confidence, classId));
        }

        return boxes;
    }

    /**
     * Closes the underlying ONNX session.
     *
     * @throws Exception if closing fails
     */
    @Override
    public void close() throws Exception {
        session.close();
    }
}
