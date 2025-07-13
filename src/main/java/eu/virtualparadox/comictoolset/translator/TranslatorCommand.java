package eu.virtualparadox.comictoolset.translator;

import com.beust.jcommander.Parameter;
import eu.virtualparadox.comictoolset.command.AbstractCommand;
import eu.virtualparadox.comictoolset.translator.translator.Translator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TranslatorCommand extends AbstractCommand {

    @Parameter(names = "--url", description = "URL to the API (eg.: http://localhost:11434/api/generate)", required = true)
    private String url;

    @Parameter(names = "--input", description = "Path to the folder of image to be translated", required = true)
    private Path inputPath;

    @Parameter(names = "--output", description = "Path to output folder", required = true)
    private Path outputPath;


    @Override
    public String getCommand() {
        return "translate";
    }

    @Override
    protected void printDetailedDescription() {

    }

    @Override
    protected void internalRun() {
        final Translator translator = new Translator(url, inputPath, outputPath);
        translator.translate();
    }

    @Override
    protected boolean validateAndPrint() {
        if (!Files.exists(inputPath)) {
            logger.error("Input path does not exist: {}", inputPath);
            return false;
        }

        if (!Files.exists(outputPath)) {
            try {
                Files.createDirectories(outputPath);
            } catch (IOException e) {
                logger.error("Failed to create output directory: {}", outputPath, e);
                return false;
            }
        }

        return true;
    }
}
