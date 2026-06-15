package com.mirceone.inventoryapp.service.ai;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Defense-in-depth: refuses to start if the local AI slot — the one used for sensitive invoice
 * processing — is configured with a cloud provider. Invoice data must never leave the device, and
 * this guarantee is enforced in code, not just by configuration discipline.
 *
 * <p>The effective local provider is {@code app.ai.local.provider} when set, otherwise the
 * top-level {@code app.ai.provider} (which already selects the on-device backend).
 */
@Component
public class LocalAiSlotValidator {

    private static final Logger log = LoggerFactory.getLogger(LocalAiSlotValidator.class);

    /** Providers that run on-device and are therefore allowed in the local slot. */
    private static final Set<String> ON_DEVICE_PROVIDERS = Set.of("mlx", "stub");

    private final AppIntegrationProperties props;

    public LocalAiSlotValidator(AppIntegrationProperties props) {
        this.props = props;
    }

    @PostConstruct
    void validate() {
        AppIntegrationProperties.Ai ai = props.getAi();
        String configured = ai.getLocal().getProvider();
        boolean explicit = configured != null && !configured.isBlank();
        String effective = (explicit ? configured : ai.getProvider()).trim().toLowerCase();

        if (!ON_DEVICE_PROVIDERS.contains(effective)) {
            throw new IllegalStateException(
                    "Local AI slot must be on-device (one of " + ON_DEVICE_PROVIDERS + "), but was '"
                            + effective + "'. Invoice extraction is pinned to the local slot and must "
                            + "never use a cloud provider. Fix app.ai.local.provider / app.ai.provider.");
        }
        log.info("Local AI slot validated as on-device (provider={})", effective);
    }
}
