package eu.virtualparadox.comictoolset.translator.bubbledetector;

import java.nio.file.Path;

public class OnnxTest {
    public static void main(String[] args) {
        try (final OnnxModelRunner omr1 = new OnnxModelRunner("src/main/resources/models/comic-speech-bubble-detector.onnx", 1024, 0.1f, true);
             final OnnxModelRunner omr2 = new OnnxModelRunner("src/main/resources/models/model_dynamic.onnx", 640, 0.1f, true)) {

            omr1.run(Path.of("src/test/resources/comics/american-vampire-01.png"));
            omr2.run(Path.of("src/test/resources/comics/american-vampire-01.png"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
