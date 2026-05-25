package com.mirceone.inventoryapp.api.ops;

import com.mirceone.inventoryapp.model.OpsEventEntity;
import com.mirceone.inventoryapp.security.AuthRateLimitFilter;
import com.mirceone.inventoryapp.security.AuthRateLimiter;
import com.mirceone.inventoryapp.security.DocumentUploadRateLimitFilter;
import com.mirceone.inventoryapp.security.OpsApiKeyFilter;
import com.mirceone.inventoryapp.security.SecurityConfig;
import com.mirceone.inventoryapp.service.ops.OpsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OpsController.class, properties = "app.ops.api-key=test-key")
@Import({SecurityConfig.class, AuthRateLimitFilter.class, DocumentUploadRateLimitFilter.class, OpsApiKeyFilter.class})
class OpsControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpsService opsService;

    @MockitoBean
    @Qualifier("authRateLimiter")
    private AuthRateLimiter authRateLimiter;

    @MockitoBean
    @Qualifier("documentUploadRateLimiter")
    private AuthRateLimiter documentUploadRateLimiter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void recentLogsWithoutApiKeyReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/ops/logs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_OPS_API_KEY"));
    }

    @Test
    void recentLogsWithApiKeyReturnsLines() throws Exception {
        when(opsService.recentLogs(10)).thenReturn(List.of("line-1", "line-2"));

        mockMvc.perform(get("/ops/logs")
                        .header("X-Ops-Api-Key", "test-key")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0]").value("line-1"));
    }

    @Test
    void recentEventsWithApiKeyReturnsPayload() throws Exception {
        OpsEventEntity event = new OpsEventEntity("prompt", "response", "gemma4:e4b");
        org.springframework.test.util.ReflectionTestUtils.setField(event, "id", java.util.UUID.randomUUID());
        org.springframework.test.util.ReflectionTestUtils.setField(
                event,
                "createdAt",
                java.time.Instant.parse("2026-01-01T00:00:00Z")
        );
        when(opsService.recentEvents(5)).thenReturn(List.of(event));

        mockMvc.perform(get("/ops/events")
                        .header("X-Ops-Api-Key", "test-key")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].model").value("gemma4:e4b"))
                .andExpect(jsonPath("$[0].promptExcerpt").value("prompt"));
    }
}
