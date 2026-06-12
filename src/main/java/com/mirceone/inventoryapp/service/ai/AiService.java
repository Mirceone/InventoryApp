package com.mirceone.inventoryapp.service.ai;

import java.util.List;

/**
 * Application-facing AI abstraction. Implementations talk to an OpenAI-compatible backend (e.g. MLX).
 */
public interface AiService {

    /**
     * Non-streaming chat completion.
     */
    String chat(List<AiChatMessage> messages);

    /**
     * Single-turn completion with JSON object response format when supported by the backend.
     */
    String chatJson(String userPrompt);
}
