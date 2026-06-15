package com.mirceone.inventoryapp.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.mirceone.inventoryapp.service.ai.AiService;
import com.mirceone.inventoryapp.service.ai.AnthropicAiService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the two AI slots.
 *
 * <ul>
 *   <li>{@code @Qualifier("localAi")} — always the on-device backend (MLX / stub). Defined by the
 *       primary {@link AiService} component; used for sensitive document processing (invoices).</li>
 *   <li>{@code @Qualifier("generalAi")} — selectable via {@code app.ai.general.provider}
 *       ({@code mlx} reuses the local backend, {@code claude} uses Anthropic). Used for general,
 *       non-sensitive functions only.</li>
 * </ul>
 */
@Configuration
public class AiSlotsConfiguration {

    @Bean
    @Qualifier("generalAi")
    AiService generalAi(@Qualifier("localAi") AiService localAi, AppIntegrationProperties props) {
        AppIntegrationProperties.Ai.General general = props.getAi().getGeneral();
        String provider = general.getProvider() == null ? "" : general.getProvider().trim().toLowerCase();
        return switch (provider) {
            case "", "mlx", "stub" -> localAi;
            case "claude", "anthropic" -> new AnthropicAiService(
                    anthropicClient(general.getClaude()), general.getClaude().getModel());
            case "openai" -> throw new IllegalStateException(
                    "app.ai.general.provider=openai is not yet implemented; use mlx or claude");
            default -> throw new IllegalStateException(
                    "Unknown app.ai.general.provider: " + general.getProvider());
        };
    }

    private static AnthropicClient anthropicClient(AppIntegrationProperties.Ai.General.Claude claude) {
        String apiKey = claude.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "app.ai.general.provider=claude requires app.ai.general.claude.api-key "
                            + "(set the ANTHROPIC_API_KEY environment variable)");
        }
        return AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }
}
