package com.mirceone.inventoryapp.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mlx", matchIfMissing = true)
public class AiClientConfiguration {

    @Bean
    OpenAIClient openAIClient(AppIntegrationProperties props) {
        AppIntegrationProperties.Ai ai = props.getAi();
        return OpenAIOkHttpClient.builder()
                .baseUrl(ai.getBaseUrl())
                .apiKey(ai.getApiKey())
                .timeout(ai.getTimeout())
                .maxRetries(ai.getMaxRetries())
                .build();
    }
}
