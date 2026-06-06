package com.incode.thirdparty.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void documentsServiceUnavailableForFreeEndpoint() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths.['/free-third-party'].get.responses.['200']")
                        .exists())
                .andExpect(jsonPath("$.paths.['/free-third-party'].get.responses.['503']")
                        .exists())
                .andExpect(jsonPath(
                                "$.paths.['/free-third-party'].get.responses.['503'].content.['application/json'].schema.$ref")
                        .value("#/components/schemas/ApiError"));
    }

    @Test
    void documentsServiceUnavailableForPremiumEndpoint() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths.['/premium-third-party'].get.responses.['200']")
                        .exists())
                .andExpect(jsonPath("$.paths.['/premium-third-party'].get.responses.['503']")
                        .exists());
    }
}
