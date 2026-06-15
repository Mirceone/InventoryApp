package com.mirceone.inventoryapp.service.ai;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionContentPart;
import com.openai.models.chat.completions.ChatCompletionContentPartImage;
import com.openai.models.chat.completions.ChatCompletionContentPartText;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import com.openai.models.chat.completions.ChatCompletionCreateParams.ResponseFormat;
import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import com.openai.models.ResponseFormatJsonObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Semaphore;

@Service
@Primary
@Qualifier("localAi")
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mlx", matchIfMissing = true)
public class OpenAiSdkAiService implements AiService {

    private final OpenAIClient client;
    private final AiModelIdResolver modelIdResolver;
    /**
     * Serializes inference against the local model. A single GPU can only run a few requests at
     * once before exhausting VRAM (Metal OOM crashes the MLX server), so concurrent classification
     * and invoice-structuring calls must queue rather than run in parallel. Default 1.
     */
    private final Semaphore inferenceGate;

    public OpenAiSdkAiService(
            OpenAIClient client,
            AiModelIdResolver modelIdResolver,
            AppIntegrationProperties props
    ) {
        this.client = client;
        this.modelIdResolver = modelIdResolver;
        this.inferenceGate = new Semaphore(Math.max(1, props.getAi().getMaxConcurrentRequests()));
    }

    @Override
    public String chat(List<AiChatMessage> messages) {
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(modelIdResolver.resolvedModelId())
                .messages(toSdkMessages(messages))
                .build();
        return extractContent(createGated(params));
    }

    @Override
    public String chatJson(String userPrompt) {
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(modelIdResolver.resolvedModelId())
                .temperature(0.0)
                .addMessage(ChatCompletionUserMessageParam.builder().content(userPrompt).build())
                .responseFormat(ResponseFormat.ofJsonObject(ResponseFormatJsonObject.builder().build()))
                .build();
        return extractContent(createGated(params));
    }

    @Override
    public String chatVision(String prompt, List<AiImage> images) {
        List<ChatCompletionContentPart> parts = new ArrayList<>(images.size() + 1);
        parts.add(ChatCompletionContentPart.ofText(
                ChatCompletionContentPartText.builder().text(prompt).build()));
        for (AiImage image : images) {
            parts.add(ChatCompletionContentPart.ofImageUrl(
                    ChatCompletionContentPartImage.builder()
                            .imageUrl(ChatCompletionContentPartImage.ImageUrl.builder()
                                    .url(toDataUrl(image))
                                    .build())
                            .build()));
        }
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(modelIdResolver.resolvedModelId())
                .temperature(0.0)
                .addMessage(ChatCompletionUserMessageParam.builder()
                        .contentOfArrayOfContentParts(parts)
                        .build())
                .build();
        return extractContent(createGated(params));
    }

    /** Runs the model call holding a single inference permit so concurrent calls queue. */
    private ChatCompletion createGated(ChatCompletionCreateParams params) {
        inferenceGate.acquireUninterruptibly();
        try {
            return client.chat().completions().create(params);
        } finally {
            inferenceGate.release();
        }
    }

    private static String toDataUrl(AiImage image) {
        String mime = image.mimeType() != null && !image.mimeType().isBlank() ? image.mimeType() : "image/png";
        return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(image.data());
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
