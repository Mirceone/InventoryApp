package com.mirceone.inventoryapp.config;

import com.mirceone.inventoryapp.ops.MaintenanceLogRing;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientsConfiguration {

    @Bean
    RestClient ollamaRestClient(RestClient.Builder builder, AppIntegrationProperties props) {
        int ms = (int) Math.min(Integer.MAX_VALUE, props.getOllama().getChatTimeout().toMillis());
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(ms);
        rf.setReadTimeout(ms);
        return builder
                .baseUrl(props.getOllama().getBaseUrl())
                .requestFactory(rf)
                .build();
    }

    @Bean
    MaintenanceLogRing maintenanceLogRing(AppIntegrationProperties props) {
        return new MaintenanceLogRing(props.getOps().getLogRingMaxLines(), props.getOps().isLogRingEnabled());
    }
}
