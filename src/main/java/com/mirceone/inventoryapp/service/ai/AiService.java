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

    /**
     * Single-turn vision completion: the prompt plus one or more images, returning the model's
     * text response. Used to transcribe scanned (image-only) documents via the VLM.
     */
    String chatVision(String prompt, List<AiImage> images);
}
