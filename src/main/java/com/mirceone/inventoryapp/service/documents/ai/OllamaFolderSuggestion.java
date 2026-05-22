package com.mirceone.inventoryapp.service.documents.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaFolderSuggestion(String folder) {
}
