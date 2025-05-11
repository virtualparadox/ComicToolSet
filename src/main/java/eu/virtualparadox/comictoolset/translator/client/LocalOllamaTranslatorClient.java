package eu.virtualparadox.comictoolset.translator.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * A generic translator that sends prompts to a local Ollama server.
 * Supports any model, language pair, and translation prompt format.
 */
@Builder
@Slf4j
public class LocalOllamaTranslatorClient {

    private final String model;
    private final String url;
    private final String sourceLanguage;
    private final String targetLanguage;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Translates the input text using the configured LLM and translation prompt.
     *
     * @param inputText text to translate
     * @return translated result
     */
    public String translate(String inputText) {
        String prompt = buildPrompt(inputText);
        OllamaRequest requestObj = new OllamaRequest(model, prompt, false);

        try {
            String requestBody = objectMapper.writeValueAsString(requestObj);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            OllamaResponse responseObj = objectMapper.readValue(response.body(), OllamaResponse.class);
            return responseObj.response().trim();

        } catch (Exception e) {
            throw new RuntimeException("Failed to translate text using local Ollama model", e);
        }
    }

    private String buildPrompt(String inputText) {
        return String.format("""
            Translate the following %s text to %s.
            
            %s
            """, sourceLanguage, targetLanguage, inputText);
    }

    // --- DTOs ---

    private record OllamaRequest(String model, String prompt, boolean stream) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OllamaResponse(String response) {}
}
