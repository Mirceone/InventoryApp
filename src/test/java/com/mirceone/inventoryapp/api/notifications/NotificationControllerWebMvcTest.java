package com.mirceone.inventoryapp.api.notifications;

import com.mirceone.inventoryapp.model.NotificationLevel;
import com.mirceone.inventoryapp.model.NotificationType;
import com.mirceone.inventoryapp.security.AuthRateLimiter;
import com.mirceone.inventoryapp.service.notifications.NotificationContracts;
import com.mirceone.inventoryapp.service.notifications.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class)
class NotificationControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    @Qualifier("authRateLimiter")
    private AuthRateLimiter authRateLimiter;

    @MockitoBean
    @Qualifier("documentUploadRateLimiter")
    private AuthRateLimiter documentUploadRateLimiter;

    @Test
    void listNotificationsReturnsInbox() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();

        when(notificationService.listNotifications(userId, true, 20)).thenReturn(
                new NotificationContracts.NotificationInbox(
                        4,
                        List.of(new NotificationContracts.NotificationSummary(
                                notificationId,
                                firmId,
                                NotificationType.PRODUCT_LOW_STOCK,
                                NotificationLevel.WARNING,
                                "Produs sub prag minim",
                                "Produsul este vizibil in buy list.",
                                Map.of("productId", UUID.randomUUID().toString()),
                                false,
                                null,
                                Instant.parse("2026-01-01T00:00:00Z")
                        ))
                )
        );

        mockMvc.perform(get("/notifications")
                        .param("unreadOnly", "true")
                        .param("limit", "20")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(4))
                .andExpect(jsonPath("$.items[0].type").value("PRODUCT_LOW_STOCK"))
                .andExpect(jsonPath("$.items[0].level").value("WARNING"))
                .andExpect(jsonPath("$.items[0].metadata.productId").exists());
    }

    @Test
    void markReadReturnsNoContent() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        doNothing().when(notificationService).markRead(userId, notificationId);

        mockMvc.perform(post("/notifications/{notificationId}/read", notificationId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isNoContent());
    }

    @Test
    void markAllReadReturnsNoContent() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(notificationService).markAllRead(userId);

        mockMvc.perform(post("/notifications/read-all")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isNoContent());
    }
}
