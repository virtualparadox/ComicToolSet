package eu.virtualparadox.comictoolset.translator.bubblecollector;

public enum BubbleModel {
    COMIC_SPEECH_BUBBLE_DETECTOR("models/bubble/comic-speech-bubble-detector.onnx", 1024),
    MODEL_DYNAMIC("models/bubble/model_dynamic.onnx", 640);

    public final String modelPath;
    public final int inputSize;

    BubbleModel(final String modelPath, final int inputSize) {
        this.modelPath = modelPath;
        this.inputSize = inputSize;

    }
}
