package eu.virtualparadox.comictoolset.translator.textremover;

import ai.onnxruntime.*;
import eu.virtualparadox.comictoolset.translator.textmaskgenerator.TextMaskBox;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static eu.virtualparadox.comictoolset.ModelExtractor.extractModelToTempFile;

/**
 * Text remover using LaMa ONNX model to inpaint detected text regions in comic images.
 * Supports tiling to process images larger than the model's 512x512 input.
 */
public class OnnxTextRemover implements TextRemover {

    private static final int TILE_SIZE = 512;

    private final OrtEnvironment env;
    private final OrtSession session;
    private final boolean debug;

    public OnnxTextRemover(final OrtEnvironment env,
                           final OrtSession session,
                           final boolean debug) {
        this.env = env;
        this.session = session;
        this.debug = debug;
    }

    @Override
    public Path removeText(final Path originalImage,
                           final List<TextMaskBox> maskBoxes) throws Exception {

        final BufferedImage fullImage = toRGBImage(ImageIO.read(originalImage.toFile()));
        final BufferedImage maskImage = generateBinaryMask(fullImage.getWidth(), fullImage.getHeight(), maskBoxes);

        final BufferedImage inpainted = new BufferedImage(fullImage.getWidth(), fullImage.getHeight(), BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < fullImage.getHeight(); y += TILE_SIZE) {
            for (int x = 0; x < fullImage.getWidth(); x += TILE_SIZE) {
                final int tileW = Math.min(TILE_SIZE, fullImage.getWidth() - x);
                final int tileH = Math.min(TILE_SIZE, fullImage.getHeight() - y);

                final BufferedImage imageTile = fullImage.getSubimage(x, y, tileW, tileH);
                final BufferedImage maskTile = maskImage.getSubimage(x, y, tileW, tileH);

                final BufferedImage paddedImage = resizeImage(imageTile, TILE_SIZE, TILE_SIZE);
                final BufferedImage paddedMask = resizeImage(maskTile, TILE_SIZE, TILE_SIZE);

                final FloatBuffer imageTensor = toFloatTensor(paddedImage, 3);
                final FloatBuffer maskTensor = toFloatTensor(paddedMask, 1);

                final OnnxTensor imageInput = OnnxTensor.createTensor(env, imageTensor, new long[]{1, 3, TILE_SIZE, TILE_SIZE});
                final OnnxTensor maskInput = OnnxTensor.createTensor(env, maskTensor, new long[]{1, 1, TILE_SIZE, TILE_SIZE});

                final OrtSession.Result result = session.run(Map.of("image", imageInput, "mask", maskInput));
                final float[][][][] output = (float[][][][]) result.get(0).getValue();

                final BufferedImage resultTile = fromFloatTensor(output);
                final BufferedImage downScaledResultTile = resizeImage(resultTile, tileW, tileH);
                inpainted.getGraphics().drawImage(downScaledResultTile, x, y, null);
            }
        }

        final Path tempFile = Files.createTempFile("inpainted", ".png");
        ImageIO.write(inpainted, "png", tempFile.toFile());
        return tempFile;
    }

    /**
     * Converts a BufferedImage to a FloatBuffer tensor in CHW format.
     */
    private FloatBuffer toFloatTensor(final BufferedImage image, final int channels) {
        final FloatBuffer buffer = FloatBuffer.allocate(channels * TILE_SIZE * TILE_SIZE);
        for (int c = 0; c < channels; c++) {
            for (int y = 0; y < TILE_SIZE; y++) {
                for (int x = 0; x < TILE_SIZE; x++) {
                    final int rgb = image.getRGB(x, y);
                    final int val = switch (c) {
                        case 0 -> (rgb >> 16) & 0xFF;
                        case 1 -> (rgb >> 8) & 0xFF;
                        case 2 -> rgb & 0xFF;
                        default -> 0;
                    };
                    buffer.put(val / 255.0f);
                }
            }
        }
        buffer.rewind();
        return buffer;
    }

    /**
     * Converts model output tensor to a BufferedImage.
     */
    private BufferedImage fromFloatTensor(final float[][][][] tensor) {
        final BufferedImage out = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                final int r = (int) tensor[0][0][y][x];
                final int g = (int) tensor[0][1][y][x];
                final int b = (int) tensor[0][2][y][x];
                out.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }
        return out;
    }

    /**
     * Generates a binary mask image from a list of text mask boxes.
     */
    private BufferedImage generateBinaryMask(final int width, final int height, final List<TextMaskBox> boxes) {
        final BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        final Graphics2D g = mask.createGraphics();
        g.setColor(Color.WHITE);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        for (final TextMaskBox box : boxes) {
            g.fillRect(box.x1, box.y1, box.x2 - box.x1, box.y2 - box.y1);
        }
        g.dispose();
        return mask;
    }

    /**
     * Converts any image type to RGB BufferedImage.
     */
    private BufferedImage toRGBImage(final BufferedImage input) {
        final BufferedImage rgbImage = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = rgbImage.createGraphics();
        g.drawImage(input, 0, 0, null);
        g.dispose();
        return rgbImage;
    }

    /**
     * Resizes an image to the target width and height.
     */
    private BufferedImage resizeImage(final BufferedImage input, final int width, final int height) {
        final BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(input, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }

    @Override
    public void close() throws Exception {
        session.close();
        env.close();
    }

    /**
     * Builder for creating {@link OnnxTextRemover} instances with optional debug output.
     */
    public static class TextRemoverBuilder {
        private TextRemoverModel model;
        private boolean debug;

        public static TextRemoverBuilder builder() {
            return new TextRemoverBuilder();
        }

        public TextRemoverBuilder model(final TextRemoverModel model) {
            this.model = model;
            return this;
        }

        public TextRemoverBuilder debug(final boolean debug) {
            this.debug = debug;
            return this;
        }

        public TextRemover build() throws Exception {
            if (model == null) {
                throw new IllegalArgumentException("TextRemoverModel must not be null");
            }
            final String extracted = extractModelToTempFile(model.modelPath);
            final OrtEnvironment env = OrtEnvironment.getEnvironment();
            final OrtSession session = env.createSession(extracted, new OrtSession.SessionOptions());
            return new OnnxTextRemover(env, session, debug);
        }
    }
}
