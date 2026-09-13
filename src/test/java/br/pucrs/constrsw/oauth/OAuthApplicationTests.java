package br.pucrs.constrsw.oauth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "keycloak.client-secret=test-client-secret")
@AutoConfigureMockMvc
class OAuthApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthAndOpenApiAreAvailable() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/login'].post").exists())
                .andExpect(jsonPath("$.paths['/login'].post.requestBody.content['multipart/form-data']").exists());
    }
}
