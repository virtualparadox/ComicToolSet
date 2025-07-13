package eu.virtualparadox.comictoolset.translator.textboxgenerator;

public enum VisualLLMModel {
    QWEN_25_3B("qwen2.5vl:3b"),
    QWEN_25_7B("qwen2.5vl:7b");

    public final String modelName;

    VisualLLMModel(final String modelName) {
        this.modelName = modelName;
    }
}
