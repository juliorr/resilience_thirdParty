package com.incode.thirdparty.api;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.incode.thirdparty.config.AppProperties;
import com.incode.thirdparty.data.CompanyDataStore;
import com.incode.thirdparty.data.FailureSimulator;
import com.incode.thirdparty.domain.Company;
import com.incode.thirdparty.domain.Source;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FreeThirdPartyController.class)
@EnableConfigurationProperties(AppProperties.class)
class FreeThirdPartyControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CompanyDataStore dataStore;

    @MockitoBean
    private FailureSimulator failureSimulator;

    @Test
    void returnsSnakeCaseCompaniesWhenAvailable() throws Exception {
        when(failureSimulator.shouldFail(anyDouble())).thenReturn(false);
        when(dataStore.search(Source.FREE, "CJ"))
                .thenReturn(List.of(new Company("CJQUNXGW", "Acme", "2021-05-01", "1 Main St", true)));

        mvc.perform(get("/free-third-party").param("query", "CJ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cin").value("CJQUNXGW"))
                .andExpect(jsonPath("$[0].registration_date").value("2021-05-01"))
                .andExpect(jsonPath("$[0].is_active").value(true));
    }

    @Test
    void returnsServiceUnavailableWhenSimulatorFails() throws Exception {
        when(failureSimulator.shouldFail(anyDouble())).thenReturn(true);

        mvc.perform(get("/free-third-party").param("query", "CJ")).andExpect(status().isServiceUnavailable());
    }
}
