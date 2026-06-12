package com.mirceone.inventoryapp.service.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Primary
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "stub")
public class StubAiService implements AiService {

    @Override
    public String chat(List<AiChatMessage> messages) {
        int count = messages == null ? 0 : messages.size();
        return "stub-ai-response messages=" + count;
    }

    @Override
    public String chatJson(String userPrompt) {
        String safe = userPrompt == null ? "" : userPrompt.replace("\"", "\\\"");
        return "{\"stub\":true,\"prompt\":\"" + safe + "\"}";
    }
}
