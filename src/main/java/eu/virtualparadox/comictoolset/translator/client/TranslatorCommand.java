package eu.virtualparadox.comictoolset.translator.client;

import com.beust.jcommander.Parameter;
import eu.virtualparadox.comictoolset.command.AbstractCommand;

public class TranslatorCommand extends AbstractCommand {

    @Parameter(
            names = "--model",
            description = "LLM model to use (eg.: mistral, llama2, etc.)",
            required = true
    )
    private String model;

    @Parameter(
            names = "--url",
            description = "URL to the API (eg.: http://localhost:11434/generate)",
            required = true
    )
    private String url;

    @Override
    public String getCommand() {
        return "translate";
    }

    @Override
    protected void printDetailedDescription() {

    }

    @Override
    protected void internalRun() {
        final LocalOllamaTranslatorClient client = LocalOllamaTranslatorClient.builder()
                .model(model)
                .url(url)
                .sourceLanguage("autoDetect")
                .targetLanguage("english")
                .build();

        final String translate = client.translate("Questa è una capra. Si erge magnificamente sulla montagna e accanto ad essa c'è un cane!");
        logger.info("==> {}", translate);
    }

    @Override
    protected boolean validateAndPrint() {
        return true;
    }
}
