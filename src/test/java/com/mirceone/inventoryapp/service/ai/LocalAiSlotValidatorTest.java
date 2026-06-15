package com.mirceone.inventoryapp.service.ai;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalAiSlotValidatorTest {

    private static LocalAiSlotValidator validatorFor(String provider, String localProvider) {
        AppIntegrationProperties props = new AppIntegrationProperties();
        props.getAi().setProvider(provider);
        props.getAi().getLocal().setProvider(localProvider);
        return new LocalAiSlotValidator(props);
    }

    @Test
    void acceptsMlxLocalSlot() {
        assertDoesNotThrow(() -> validatorFor("mlx", "").validate());
    }

    @Test
    void acceptsStubLocalSlot() {
        assertDoesNotThrow(() -> validatorFor("stub", "").validate());
    }

    @Test
    void rejectsCloudProviderInLocalSlotViaExplicitOverride() {
        LocalAiSlotValidator validator = validatorFor("mlx", "claude");
        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("on-device"));
    }

    @Test
    void rejectsCloudProviderInLocalSlotViaTopLevelProvider() {
        LocalAiSlotValidator validator = validatorFor("openai", "");
        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void explicitLocalProviderOverridesTopLevel() {
        // Top-level says stub (on-device) but the explicit local slot is cloud → must reject.
        assertThrows(IllegalStateException.class, () -> validatorFor("stub", "anthropic").validate());
    }
}
