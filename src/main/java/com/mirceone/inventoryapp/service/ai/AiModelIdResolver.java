package com.mirceone.inventoryapp.service.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mlx", matchIfMissing = true)
public class AiModelIdResolver {

    private final MlxCommandResolver mlxCommandResolver;

    public AiModelIdResolver(MlxCommandResolver mlxCommandResolver) {
        this.mlxCommandResolver = mlxCommandResolver;
    }

    public String resolvedModelId() {
        return mlxCommandResolver.resolvedApiModelId();
    }
}
