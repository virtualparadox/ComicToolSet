package eu.virtualparadox.comictoolset.onnxmodelinfo;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.PathConverter;
import eu.virtualparadox.comictoolset.command.AbstractCommand;

import java.nio.file.Path;
import java.util.Map;

public class OnnxModelInfoCommand extends AbstractCommand {

    @Parameter(
            names = "--model",
            description = "Path to the model (eg.: /var/lib/model.onnx)",
            required = true,
            converter = PathConverter.class
    )
    private Path modelPath;

    @Override
    public String getCommand() {
        return "onnxmodelinfo";
    }

    @Override
    protected void printDetailedDescription() {

    }

    @Override
    protected void internalRun() {
        try {
            final OrtEnvironment env = OrtEnvironment.getEnvironment();
            try (OrtSession session = env.createSession(modelPath.toString(), new OrtSession.SessionOptions())) {

                for (Map.Entry<String, NodeInfo> entry : session.getInputInfo().entrySet()) {
                    System.out.println("Input: " + entry.getKey() + " -> " + entry.getValue().getInfo());
                }
                for (Map.Entry<String, NodeInfo> entry : session.getOutputInfo().entrySet()) {
                    System.out.println("Output: " + entry.getKey() + " -> " + entry.getValue().getInfo());
                }
            }
        } catch (Exception e) {
            logger.error("Error while loading the model: {}", e.getMessage());
            throw new IllegalStateException("Unable to load model", e);
        }
    }

    @Override
    protected boolean validateAndPrint() {
        return true;
    }
}
