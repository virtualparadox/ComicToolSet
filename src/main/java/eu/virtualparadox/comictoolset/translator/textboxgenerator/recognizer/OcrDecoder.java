package eu.virtualparadox.comictoolset.translator.textboxgenerator.recognizer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class OcrDecoder {

    /**
     * Loads a list of labels from a PaddleOCR key file (e.g., ppocr_keys_v1.txt).
     * Each line of the file represents one label.
     *
     * @param resourcePath path inside classpath (e.g. "models/recognizer/ppocr_keys_v1.txt")
     * @return List of labels
     */
    public static List<String> loadLabelList(final String resourcePath) {
        final List<String> labels = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                OcrDecoder.class.getClassLoader().getResourceAsStream(resourcePath)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                labels.add(line.trim());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load label list from: " + resourcePath, e);
        }
        return labels;
    }

    /**
     * Performs greedy CTC decoding on a time-distributed logit matrix.
     *
     * @param logits     the output logits from the OCR model; shape: [timeSteps][numClasses]
     * @param labelList  list of class labels, where index 0 is reserved for the blank token
     * @return the decoded string
     */
    public static String ctcDecode(final float[][] logits, final List<String> labelList) {
        final StringBuilder sb = new StringBuilder();
        int lastIndex = -1;

        for (int t = 0; t < logits.length; t++) {
            int maxIndex = argmax(logits[t]);

            // Skip repeated characters and blank tokens (index 0)
            if (maxIndex != 0 && maxIndex != lastIndex) {
                try {
                    sb.append(labelList.get(maxIndex));
                }
                catch (Exception e) {
                    sb.append(" ");
                }
            }

            lastIndex = maxIndex;
        }

        return sb.toString();
    }

    /**
     * Finds the index of the maximum value in an array.
     *
     * @param array an array of floats
     * @return the index of the maximum element
     */
    private static int argmax(final float[] array) {
        int maxIndex = 0;
        float maxVal = array[0];

        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxVal) {
                maxVal = array[i];
                maxIndex = i;
            }
        }

        return maxIndex;
    }
}