package com.mirceone.inventoryapp.service.ai;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MlxServerManagerTest {

    @Test
    void resolvePortPrefersExplicitConfig() {
        AppIntegrationProperties.Ai ai = new AppIntegrationProperties.Ai();
        ai.setServerPort(9001);
        ai.setBaseUrl("http://127.0.0.1:8000/v1");

        assertEquals(9001, MlxServerManager.resolvePort(ai));
    }

    @Test
    void resolvePortFromBaseUrlWhenServerPortUnset() {
        AppIntegrationProperties.Ai ai = new AppIntegrationProperties.Ai();
        ai.setServerPort(0);
        ai.setBaseUrl("http://127.0.0.1:8000/v1");

        assertEquals(8000, MlxServerManager.resolvePort(ai));
    }

    @Test
    void modelsUrlAppendsModelsPath() {
        AppIntegrationProperties.Ai ai = new AppIntegrationProperties.Ai();
        ai.setBaseUrl("http://127.0.0.1:8000/v1/");

        assertEquals("http://127.0.0.1:8000/v1/models", MlxServerManager.modelsUrl(ai));
    }
}
