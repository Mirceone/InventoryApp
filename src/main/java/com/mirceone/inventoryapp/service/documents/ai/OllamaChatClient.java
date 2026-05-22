package com.mirceone.inventoryapp.service.documents.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class OllamaChatClient {

    private final RestClient ollamaRestClient;
    private final AppIntegrationProperties props;
    private final ObjectMapper objectMapper;

    public OllamaChatClient(
            @Qualifier("ollamaRestClient") RestClient ollamaRestClient,
            AppIntegrationProperties props,
            ObjectMapper objectMapper
    ) {
        this.ollamaRestClient = ollamaRestClient;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public String chatJson(String userPrompt) {
        OllamaChatRequest request = new OllamaChatRequest(
                props.getOllama().getModel(),
                false,
                List.of(new OllamaChatRequest.OllamaMessage("user", userPrompt)),
                "json"
        );
        OllamaChatResponse response = ollamaRestClient.post()
                .uri("/api/chat")
                .body(request)
                .retrieve()
                .body(OllamaChatResponse.class);
        if (response == null || response.message() == null || response.message().content() == null) {
            throw new IllegalStateException("Empty Ollama response");
        }
        return response.message().content().strip();
    }

    public OllamaFolderSuggestion parseFolderSuggestion(String jsonContent) {
        try {
            return objectMapper.readValue(jsonContent, OllamaFolderSuggestion.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Ollama JSON: " + jsonContent, e);
        }
    }
}
