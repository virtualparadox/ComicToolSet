package eu.virtualparadox.comictoolset.translator.bubbledetector;

import ai.onnxruntime.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An ONNX model runner that detects comic speech bubbles in images using a pre-trained ONNX model.
 * <p>
 * This class supports preprocessing, running inference, and optional debug rendering with bounding boxes.
 * The model file is expected to be located within the JAR resources.
 */
@Builder
@Accessors(fluent = true)
public final class OnnxModelRunner implements AutoCloseable {

    /** Path to the ONNX model resource (inside the JAR). */
    private final String modelFilename;

    /** Input image size (e.g. 640 or 1024). Assumes square input. */
    private final int inputSize;

    /** Minimum confidence threshold for detections to be considered valid. */
    private final float confidenceThreshold;

    /** Whether to enable debug mode (generates annotated image with bounding boxes). */
    private final boolean debug;

    /** ONNX runtime environment instance. */
    private OrtEnvironment env;

    /** ONNX inference session. */
    private OrtSession session;

    /** Optional debugger for drawing results on the image. */
    private ComicBubbleBoxDebugger debugger;

    /** Private constructor used by builder. */
    private OnnxModelRunner(final String modelFilename,
                            final int inputSize,
                            final float confidenceThreshold,
                            final boolean debug) {
        this.modelFilename = modelFilename;
        this.inputSize = inputSize;
        this.confidenceThreshold = confidenceThreshold;
        this.debug = debug;
    }

    /**
     * Builder customization to initialize ONNX environment, session, and debugger during build.
     */
    public static class OnnxModelRunnerBuilder {
        public OnnxModelRunner build() {
            // check the inputs
            if (modelFilename == null || modelFilename.isEmpty()) {
                throw new IllegalArgumentException("Model filename cannot be null or empty");
            }

            if (inputSize <= 0) {
                throw new IllegalArgumentException("Input size must be a positive integer");
            }

            if (confidenceThreshold < 0 || confidenceThreshold > 1) {
                throw new IllegalArgumentException("Confidence threshold must be between 0 and 1");
            }

            // initialize the ONNX environment and session
            try {
                final OnnxModelRunner runner = new OnnxModelRunner(modelFilename, inputSize, confidenceThreshold, debug);
                final String tempModelFilename = runner.extractModelToTempFile(modelFilename);

                runner.env = OrtEnvironment.getEnvironment();
                runner.session = runner.env.createSession(tempModelFilename, new OrtSession.SessionOptions());
                runner.debugger = new ComicBubbleBoxDebugger();

                return runner;
            } catch (final Exception e) {
                throw new RuntimeException("Failed to initialize OnnxModelRunner", e);
            }
        }
    }

    /**
     * Extracts the model resource from the JAR to a temporary file on disk for ONNX Runtime compatibility.
     *
     * @param modelFilename model path relative to the resource root (e.g. "models/bubble.onnx")
     * @return absolute path to the extracted temporary model file
     */
    private String extractModelToTempFile(final String modelFilename) {
        try (final InputStream in = getClass().getClassLoader().getResourceAsStream(modelFilename)) {
            if (in == null) {
                throw new IllegalArgumentException("Model resource not found: " + modelFilename);
            }

            final Path tempFile = Files.createTempFile("onnx-model-", ".onnx");
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            tempFile.toFile().deleteOnExit();
            return tempFile.toAbsolutePath().toString();

        } catch (final IOException e) {
            throw new RuntimeException("Failed to extract ONNX model from JAR: " + modelFilename, e);
        }
    }

    /**
     * Runs inference on a given image path and returns a list of bounding boxes.
     *
     * @param imagePath path to an image file
     * @return list of detected {@link ComicBubbleBox} instances
     * @throws Exception if the image cannot be loaded or ONNX inference fails
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

        if (outputObj instanceof final float[][][] output) {
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
     * Transposes a matrix (rows become columns).
     * Used to adapt different ONNX output layouts.
     *
     * @param original original matrix [N x M]
     * @return transposed matrix [M x N]
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
     * Resizes a {@link BufferedImage} using bilinear interpolation.
     *
     * @param originalImage the image to resize
     * @param width         target width
     * @param height        target height
     * @return resized image in RGB format
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
     * Converts a resized RGB image into a normalized float tensor (NCHW format).
     *
     * @param image input image (already resized)
     * @param size  expected input dimension
     * @return float tensor normalized to [0,1] range
     */
    private float[] preprocessImage(final BufferedImage image,
                                    final int size) {
        final float[] tensor = new float[3 * size * size];
        final int[] pixels = image.getRGB(0, 0, size, size, null, 0, size);
        for (int i = 0; i < size * size; i++) {
            final int rgb = pixels[i];
            tensor[i] = ((rgb >> 16) & 0xFF) / 255.0f;
            tensor[i + size * size] = ((rgb >> 8) & 0xFF) / 255.0f;
            tensor[i + 2 * size * size] = (rgb & 0xFF) / 255.0f;
        }
        return tensor;
    }

    /**
     * Converts the raw model outputs into a list of bounding boxes.
     *
     * @param detections raw ONNX outputs (shape: N x 6)
     * @param origWidth  original image width before resizing
     * @param origHeight original image height before resizing
     * @return list of {@link ComicBubbleBox} with scaled coordinates
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

            boxes.add(new ComicBubbleBox((int) x1, (int) y1, (int) x2, (int) y2, confidence, classId));
        }

        return boxes;
    }

    /**
     * Closes the ONNX session. This should be called explicitly or via try-with-resources.
     *
     * @throws Exception if session closing fails
     */
    @Override
    public void close() throws Exception {
        session.close();
    }
}
