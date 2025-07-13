package eu.virtualparadox.comictoolset.translator.textmaskgenerator;

public enum TextMaskModel {
    SMALL("models/paddle/ch_PP-OCRv4_det_infer.onnx"),
    LARGE("models/paddle/ch_PP-OCRv4_det_server_infer.onnx");

    public final String modelPath;

    TextMaskModel(String modelPath) {
        this.modelPath = modelPath;
    }
}
