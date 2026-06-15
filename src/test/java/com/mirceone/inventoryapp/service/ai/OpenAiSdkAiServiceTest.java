package com.mirceone.inventoryapp.service.ai;

import com.openai.models.chat.completions.ChatCompletionMessageParam;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenAiSdkAiServiceTest {

    @Test
    void mapsMessagesToSdkParams() {
        List<ChatCompletionMessageParam> mapped = OpenAiSdkAiService.toSdkMessages(List.of(
                AiChatMessage.system("sys"),
                AiChatMessage.user("usr"),
                AiChatMessage.assistant("asst")
        ));
        assertEquals(3, mapped.size());
    }
}
