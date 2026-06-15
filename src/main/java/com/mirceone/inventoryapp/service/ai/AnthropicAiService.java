package com.mirceone.inventoryapp.service.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;

import java.util.List;

/**
 * Cloud {@link AiService} backed by Anthropic's API (Claude). Used only for the "general"
 * (non-sensitive) AI slot — it is never wired into invoice extraction, which is pinned to the
 * on-device local slot. Not a Spring component: instances are created by
 * {@code AiSlotsConfiguration} only when {@code app.ai.general.provider=claude}.
 */
public class AnthropicAiService implements AiService {

    private static final long MAX_TOKENS = 4096L;

    private final AnthropicClient client;
    private final String model;

    public AnthropicAiService(AnthropicClient client, String model) {
        this.client = client;
        this.model = model;
    }

    @Override
    public String chat(List<AiChatMessage> messages) {
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS);
        StringBuilder system = new StringBuilder();
        for (AiChatMessage message : messages) {
            switch (message.role()) {
                case "system" -> {
                    if (!system.isEmpty()) {
                        system.append("\n\n");
                    }
                    system.append(message.content());
                }
                case "assistant" -> builder.addAssistantMessage(message.content());
                default -> builder.addUserMessage(message.content());
            }
        }
        if (!system.isEmpty()) {
            builder.system(system.toString());
        }
        return extractText(client.messages().create(builder.build()));
    }

    @Override
    public String chatJson(String userPrompt) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .system("Respond with a single valid JSON object and nothing else. "
                        + "Do not wrap it in markdown code fences.")
                .addUserMessage(userPrompt)
                .build();
        return extractText(client.messages().create(params));
    }

    @Override
    public String chatVision(String prompt, List<AiImage> images) {
        // Vision is only used for sensitive invoice transcription, which is pinned to the local
        // on-device slot. The cloud general slot deliberately does not handle document images.
        throw new UnsupportedOperationException(
                "Vision is not available on the cloud general AI slot; use the local slot");
    }

    private static String extractText(Message message) {
        String text = message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(block -> block.text())
                .reduce("", (a, b) -> a + b)
                .strip();
        if (text.isEmpty()) {
            throw new IllegalStateException("Empty AI response: no text content");
        }
        return text;
    }
}
