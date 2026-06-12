package com.mirceone.inventoryapp.service.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Primary
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "stub")
public class StubAiService implements AiService {

    private static final Pattern CATCH_ALL_PROMPT = Pattern.compile(
            "If unsure, use ([^.\n]+)\\.",
            Pattern.MULTILINE
    );

    @Override
    public String chat(List<AiChatMessage> messages) {
        int count = messages == null ? 0 : messages.size();
        return "stub-ai-response messages=" + count;
    }

    @Override
    public String chatJson(String userPrompt) {
        if (userPrompt != null && userPrompt.contains("Classify this file")) {
            String folder = "Misc";
            Matcher matcher = CATCH_ALL_PROMPT.matcher(userPrompt);
            if (matcher.find()) {
                folder = matcher.group(1).strip();
            }
            return "{\"folder\":\"" + folder.replace("\"", "\\\"") + "\"}";
        }
        if (userPrompt != null && userPrompt.contains("Extract the structured data from this invoice")) {
            return """
                    {"supplier":"Stub Supplier SRL","invoiceNumber":"STUB-001",\
                    "invoiceDate":"2026-01-01","currency":"RON","total":100.00,\
                    "lineItems":[{"description":"Stub Product","sku":"STUB-SKU",\
                    "quantity":2,"unit":"buc","unitPrice":50.00,"lineTotal":100.00}]}""";
        }
        String safe = userPrompt == null ? "" : userPrompt.replace("\"", "\\\"");
        return "{\"stub\":true,\"prompt\":\"" + safe + "\"}";
    }
}
