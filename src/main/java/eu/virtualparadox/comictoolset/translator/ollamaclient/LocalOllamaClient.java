package eu.virtualparadox.comictoolset.translator.ollamaclient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * A generic translator that sends prompts to a local Ollama server.
 * Supports any model, language pair, and translation prompt format.
 */
@Builder
@Slf4j
public class LocalOllamaClient {

    private final String model;
    private final String url;
    private final String sourceLanguage;
    private final String targetLanguage;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Translates the input originalText using the configured LLM and translation prompt.
     *
     * @param inputText originalText to translate
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
            throw new RuntimeException("Failed to translate originalText using local Ollama model", e);
        }
    }

    /**
     * Sends an image to the Ollama endpoint and requests speech bubble text extraction.
     *
     * @param base64Image the base64 encoded image
     * @return extracted text from the speech bubble
     */
    public String extract(final String base64Image) {
        try {

            String prompt = """
                    Extract the text from the speech bubbles in this comic image.
                    Please follow these rules:
                    - If there is no text in the image, return an empty JSON array!
                    - Remove line breaks and hyphenation caused by text wrapping.
                    - Do not hallucinate text or interpret meaning; only extract what is visually present in the image.

                    Return the result as a JSON array of objects with the following fields (in case of no bubbles, empty array):
                    [
                            {
                            "text": "<extracted full sentence without line breaks or hyphenation artifacts>",
                            "language": "<3 character long language code>",
                            }
                    ]
                    """;

            String requestBody = String.format("""
                    {
                      "model": "%s",
                      "stream": false,
                      "prompt": "%s",
                      "images": ["%s"]
                    }
                    """, model, prompt.replace("\"", "\\\"").replace("\n", ""), base64Image);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            OllamaResponse responseObj = objectMapper.readValue(response.body(), OllamaResponse.class);
            if (response.statusCode() != 200) {
                log.error("Failed to extract text from image: " + response.body());
            }
            return responseObj.response().trim();

        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text from image using local Ollama model", e);
        }
    }

    private String buildPrompt(String inputText) {
        return String.format("""
                Translate the following %s originalText to %s. Don't add explanation. Only pure translations. Do not add any extra text or notes.  
                Here is the text: 
                %s
                """, sourceLanguage, targetLanguage, inputText);
    }

    // --- DTOs ---

    private record OllamaRequest(String model, String prompt, boolean stream) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OllamaResponse(String response) {
    }
}
