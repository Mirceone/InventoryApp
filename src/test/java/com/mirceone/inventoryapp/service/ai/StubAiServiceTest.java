package com.mirceone.inventoryapp.service.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StubAiServiceTest {

    private final StubAiService service = new StubAiService();

    @Test
    void chatReturnsDeterministicPlaceholder() {
        String response = service.chat(List.of(
                AiChatMessage.user("hello"),
                AiChatMessage.system("be helpful")
        ));
        assertEquals("stub-ai-response messages=2", response);
    }

    @Test
    void chatJsonReturnsStubJson() {
        String json = service.chatJson("classify this file");
        assertTrue(json.contains("\"stub\":true"));
        assertTrue(json.contains("classify this file"));
    }
}
