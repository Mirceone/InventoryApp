package com.mirceone.inventoryapp.service.documents.ai;

record OllamaChatResponse(OllamaMessage message) {
    record OllamaMessage(String role, String content) {
    }
}
