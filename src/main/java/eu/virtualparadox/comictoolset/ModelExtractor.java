package eu.virtualparadox.comictoolset;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ModelExtractor {

    private ModelExtractor() {
        // Prevent instantiation
    }

    /**
     * Extracts the model file from the JAR classpath to a temp location.
     * @param resourcePath the model resource path (e.g., "models/paddle/model.onnx")
     * @return path to extracted temporary file
     */
    public static String extractModelToTempFile(final String resourcePath) {
        try (final InputStream in = ModelExtractor.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalArgumentException("Model resource not found: " + resourcePath);
            }
            final Path tempFile = Files.createTempFile("onnx-model-", ".onnx");
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            tempFile.toFile().deleteOnExit();
            return tempFile.toAbsolutePath().toString();
        } catch (final IOException e) {
            throw new RuntimeException("Failed to extract ONNX model from JAR: " + resourcePath, e);
        }
    }

}
