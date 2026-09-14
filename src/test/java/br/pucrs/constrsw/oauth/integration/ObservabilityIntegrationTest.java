package br.pucrs.constrsw.oauth.integration;

import br.pucrs.constrsw.oauth.service.KeycloakService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ObservabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry meterRegistry;

    @MockBean
    private KeycloakService keycloakService;

    @Test
    @DisplayName("Observabilidade: MeterRegistry deve estar injetado e ativo no contexto")
    void testMeterRegistryInjected() {
        assertNotNull(meterRegistry, "MeterRegistry deve estar configurado no Spring Context");
    }

    @Test
    @DisplayName("Observabilidade: Indicadores de negócio devem ser registrados após chamadas HTTP")
    void testMetricsRecordedAfterCalls() throws Exception {
        when(keycloakService.isTokenValidWithKeycloak(anyString())).thenReturn(true);
        when(keycloakService.extractUsername(anyString())).thenReturn("prof_user");
        when(keycloakService.extractRoles(anyString())).thenReturn(List.of("professor"));
        when(keycloakService.hasAccessToResource(List.of("professor"), "lessons")).thenReturn(true);

        double initialAllowed = meterRegistry.counter("oauth.validations.total", "status", "allowed", "resource", "lessons").count();

        mockMvc.perform(post("/validate")
                        .header("Authorization", "Bearer valid-prof-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resource\":\"lessons\"}"))
                .andExpect(status().isOk());

        double updatedAllowed = meterRegistry.counter("oauth.validations.total", "status", "allowed", "resource", "lessons").count();
        assertEquals(initialAllowed + 1.0, updatedAllowed, "O contador oauth.validations.total deve incrementar");

        // Teste de negação por missing header
        double initialDenied = meterRegistry.counter("oauth.validations.denied", "reason", "missing_header").count();

        mockMvc.perform(post("/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resource\":\"lessons\"}"))
                .andExpect(status().isForbidden());

        double updatedDenied = meterRegistry.counter("oauth.validations.denied", "reason", "missing_header").count();
        assertEquals(initialDenied + 1.0, updatedDenied, "O contador oauth.validations.denied deve incrementar");
    }
}
