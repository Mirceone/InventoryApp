package com.mirceone.inventoryapp.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirceone.inventoryapp.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
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

    @Test
    void signupReturnsTokenPair() throws Exception {
        SignupRequest request = new SignupRequest("john@example.com", "password123", "John");
        AuthResponse response = new AuthResponse("Bearer", "access-token", 3600L, "refresh-token", 1209600L);
        when(authService.signup(any(SignupRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void loginReturnsTokenPair() throws Exception {
        LoginRequest request = new LoginRequest("john@example.com", "password123");
        AuthResponse response = new AuthResponse("Bearer", "access-token", 3600L, "refresh-token", 1209600L);
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void refreshReturnsTokenPair() throws Exception {
        RefreshRequest request = new RefreshRequest("refresh-token");
        AuthResponse response = new AuthResponse("Bearer", "new-access", 3600L, "new-refresh", 1209600L);
        when(authService.refresh(any(RefreshRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    void logoutReturnsOk() throws Exception {
        LogoutRequest request = new LogoutRequest("refresh-token");
        doNothing().when(authService).logout(any(LogoutRequest.class));

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void signupWithInvalidPayloadReturnsValidationError() throws Exception {
        SignupRequest invalid = new SignupRequest("not-an-email", "short", "John");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed"));
    }

    @Test
    void refreshWithInvalidTokenReturnsBusinessError() throws Exception {
        RefreshRequest request = new RefreshRequest("bad-refresh-token");
        when(authService.refresh(any(RefreshRequest.class)))
                .thenThrow(new ResponseStatusException(UNAUTHORIZED, "Invalid refresh token"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));
    }

}
