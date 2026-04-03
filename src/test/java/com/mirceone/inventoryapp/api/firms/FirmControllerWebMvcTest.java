package com.mirceone.inventoryapp.api.firms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirceone.inventoryapp.service.FirmService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FirmController.class)
class FirmControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FirmService firmService;

    @Test
    void createFirmReturnsCreatedFirm() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        CreateFirmRequest request = new CreateFirmRequest("Demo SRL");
        FirmResponse response = new FirmResponse(firmId, "Demo SRL");

        when(firmService.createFirm(eq(userId), any(CreateFirmRequest.class))).thenReturn(response);

        mockMvc.perform(post("/firms")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firmId.toString()))
                .andExpect(jsonPath("$.name").value("Demo SRL"));
    }

    @Test
    void listFirmsReturnsUserFirms() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID firmId1 = UUID.randomUUID();
        UUID firmId2 = UUID.randomUUID();

        when(firmService.getFirmsForUser(userId)).thenReturn(List.of(
                new FirmResponse(firmId1, "Firm One"),
                new FirmResponse(firmId2, "Firm Two")
        ));

        mockMvc.perform(get("/firms")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(firmId1.toString()))
                .andExpect(jsonPath("$[0].name").value("Firm One"))
                .andExpect(jsonPath("$[1].id").value(firmId2.toString()))
                .andExpect(jsonPath("$[1].name").value("Firm Two"));
    }

    @Test
    void createFirmWithInvalidPayloadReturnsValidationError() throws Exception {
        UUID userId = UUID.randomUUID();
        CreateFirmRequest invalid = new CreateFirmRequest("");

        mockMvc.perform(post("/firms")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed"));
    }

    @Test
    void listFirmsWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/firms"))
                .andExpect(status().isUnauthorized());
    }
}
