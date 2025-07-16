package eu.virtualparadox.comictoolset.translator.textremover;

public enum TextRemoverModel {

    LAMA_FP32("models/inpaint/lama_fp32.onnx");

    public final String modelPath;

    TextRemoverModel(String modelPath) {
        this.modelPath = modelPath;
    }
}
