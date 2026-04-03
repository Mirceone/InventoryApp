package com.mirceone.inventoryapp.api.auth;

import com.mirceone.inventoryapp.model.ProviderType;
import com.mirceone.inventoryapp.security.AuthRateLimiter;
import com.mirceone.inventoryapp.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
class AuthMeWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AuthRateLimiter authRateLimiter;

    @Test
    void meReturnsCurrentUser() throws Exception {
        UUID userId = UUID.randomUUID();
        MeResponse me = new MeResponse(userId, "john@example.com", "John", ProviderType.LOCAL);
        when(authService.getMe(userId)).thenReturn(me);

        mockMvc.perform(get("/auth/me")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.displayName").value("John"))
                .andExpect(jsonPath("$.provider").value("LOCAL"));
    }

    @Test
    void logoutAllReturnsOk() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(authService).logoutAll(userId);

        mockMvc.perform(post("/auth/logout-all")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isOk());
    }
}
