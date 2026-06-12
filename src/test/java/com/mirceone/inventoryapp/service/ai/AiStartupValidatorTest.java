package com.mirceone.inventoryapp.service.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class AiStartupValidatorTest {

    @Test
    void probeModelsReturnsFalseForUnreachableHost() {
        assertFalse(AiStartupValidator.probeModels("http://127.0.0.1:1/v1/models", "local"));
    }
}
