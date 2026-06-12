package com.mirceone.inventoryapp.service.ai;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import com.openai.models.chat.completions.ChatCompletionCreateParams.ResponseFormat;
import com.openai.models.ResponseFormatJsonObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Primary
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mlx", matchIfMissing = true)
public class OpenAiSdkAiService implements AiService {

    private final OpenAIClient client;
    private final AiModelIdResolver modelIdResolver;

    public OpenAiSdkAiService(OpenAIClient client, AiModelIdResolver modelIdResolver) {
        this.client = client;
        this.modelIdResolver = modelIdResolver;
    }

    @Override
    public String chat(List<AiChatMessage> messages) {
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(modelIdResolver.resolvedModelId())
                .messages(toSdkMessages(messages))
                .build();
        return extractContent(client.chat().completions().create(params));
    }

    @Override
    public String chatJson(String userPrompt) {
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(modelIdResolver.resolvedModelId())
                .addMessage(ChatCompletionUserMessageParam.builder().content(userPrompt).build())
                .responseFormat(ResponseFormat.ofJsonObject(ResponseFormatJsonObject.builder().build()))
                .build();
        return extractContent(client.chat().completions().create(params));
    }

    static List<ChatCompletionMessageParam> toSdkMessages(List<AiChatMessage> messages) {
        List<ChatCompletionMessageParam> sdkMessages = new ArrayList<>(messages.size());
        for (AiChatMessage message : messages) {
            sdkMessages.add(toSdkMessage(message));
        }
        return sdkMessages;
    }

    private static ChatCompletionMessageParam toSdkMessage(AiChatMessage message) {
        return switch (message.role()) {
            case "system" -> ChatCompletionMessageParam.ofSystem(
                    ChatCompletionSystemMessageParam.builder().content(message.content()).build());
            case "assistant" -> ChatCompletionMessageParam.ofAssistant(
                    ChatCompletionAssistantMessageParam.builder().content(message.content()).build());
            default -> ChatCompletionMessageParam.ofUser(
                    ChatCompletionUserMessageParam.builder().content(message.content()).build());
        };
    }

    private static String extractContent(ChatCompletion completion) {
        if (completion.choices() == null || completion.choices().isEmpty()) {
            throw new IllegalStateException("Empty AI response: no choices");
        }
        var message = completion.choices().getFirst().message();
        if (message == null || message.content().isEmpty()) {
            throw new IllegalStateException("Empty AI response: no message content");
        }
        return message.content().get().strip();
    }
}
