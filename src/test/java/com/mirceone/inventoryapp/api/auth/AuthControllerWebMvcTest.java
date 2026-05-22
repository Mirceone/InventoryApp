package com.mirceone.inventoryapp.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirceone.inventoryapp.security.AuthRateLimiter;
import com.mirceone.inventoryapp.service.auth.AuthContracts;
import com.mirceone.inventoryapp.service.auth.AuthService;
import com.mirceone.inventoryapp.service.auth.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    @Qualifier("authRateLimiter")
    private AuthRateLimiter authRateLimiter;

    @MockitoBean
    @Qualifier("documentUploadRateLimiter")
    private AuthRateLimiter documentUploadRateLimiter;

    @Test
    void signupReturnsTokenPair() throws Exception {
        AuthContracts.IssuedTokenPair response =
                new AuthContracts.IssuedTokenPair("Bearer", "access-token", 3600L, "refresh-token", 1209600L);
        when(authService.signup(any(AuthContracts.SignupSpec.class))).thenReturn(response);

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("john@example.com", "password123", "John")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void loginReturnsTokenPair() throws Exception {
        AuthContracts.IssuedTokenPair response =
                new AuthContracts.IssuedTokenPair("Bearer", "access-token", 3600L, "refresh-token", 1209600L);
        when(authService.login(any(AuthContracts.LoginSpec.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("john@example.com", "password123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void refreshReturnsTokenPair() throws Exception {
        AuthContracts.IssuedTokenPair response =
                new AuthContracts.IssuedTokenPair("Bearer", "new-access", 3600L, "new-refresh", 1209600L);
        when(authService.refresh(any(AuthContracts.RefreshSpec.class))).thenReturn(response);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson("refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    void logoutReturnsOk() throws Exception {
        doNothing().when(authService).logout(any(AuthContracts.LogoutSpec.class));

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson("refresh-token")))
                .andExpect(status().isOk());
    }

    @Test
    void signupWithInvalidPayloadReturnsValidationError() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("not-an-email", "short", "John")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed"));
    }

    @Test
    void forgotPasswordReturnsNoContent() throws Exception {
        doNothing().when(passwordResetService).requestReset(any(AuthContracts.ForgotPasswordSpec.class));

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "john@example.com"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void resetPasswordReturnsNoContent() throws Exception {
        doNothing().when(passwordResetService).resetPassword(any(AuthContracts.CompletePasswordResetSpec.class));

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", "reset-token",
                                "newPassword", "newpassword123"
                        ))))
                .andExpect(status().isNoContent());
    }

    @Test
    void refreshWithInvalidTokenReturnsBusinessError() throws Exception {
        when(authService.refresh(any(AuthContracts.RefreshSpec.class)))
                .thenThrow(new ResponseStatusException(UNAUTHORIZED, "Invalid refresh token"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson("bad-refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));
    }

    private String signupJson(String email, String password, String displayName) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", password,
                "displayName", displayName
        ));
    }

    private String loginJson(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", password
        ));
    }

    private String refreshJson(String refreshToken) throws Exception {
        return objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken));
    }
}
