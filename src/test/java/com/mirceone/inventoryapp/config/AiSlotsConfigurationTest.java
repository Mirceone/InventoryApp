package com.mirceone.inventoryapp.config;

import com.mirceone.inventoryapp.service.ai.AiChatMessage;
import com.mirceone.inventoryapp.service.ai.AiImage;
import com.mirceone.inventoryapp.service.ai.AiService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiSlotsConfigurationTest {

    private final AiSlotsConfiguration config = new AiSlotsConfiguration();

    private static final AiService LOCAL = new AiService() {
        @Override public String chat(List<AiChatMessage> messages) {
            return "local";
        }
        @Override public String chatJson(String userPrompt) {
            return "local";
        }
        @Override public String chatVision(String prompt, List<AiImage> images) {
            return "local";
        }
    };

    private static AppIntegrationProperties propsWithGeneral(String provider, String apiKey) {
        AppIntegrationProperties props = new AppIntegrationProperties();
        props.getAi().getGeneral().setProvider(provider);
        props.getAi().getGeneral().getClaude().setApiKey(apiKey);
        return props;
    }

    @Test
    void mlxGeneralProviderReusesLocalSlot() {
        AiService general = config.generalAi(LOCAL, propsWithGeneral("mlx", ""));
        assertSame(LOCAL, general, "general=mlx must delegate to the local on-device backend");
    }

    @Test
    void blankGeneralProviderReusesLocalSlot() {
        assertSame(LOCAL, config.generalAi(LOCAL, propsWithGeneral("", "")));
    }

    @Test
    void claudeWithoutApiKeyFailsFast() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> config.generalAi(LOCAL, propsWithGeneral("claude", "")));
        assertTrue(ex.getMessage().contains("api-key"));
    }

    @Test
    void openaiGeneralProviderIsRejectedUntilImplemented() {
        assertThrows(IllegalStateException.class,
                () -> config.generalAi(LOCAL, propsWithGeneral("openai", "")));
    }

    @Test
    void unknownGeneralProviderIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> config.generalAi(LOCAL, propsWithGeneral("gemini", "")));
    }
}
