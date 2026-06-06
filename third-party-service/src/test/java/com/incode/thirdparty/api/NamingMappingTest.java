package com.incode.thirdparty.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.thirdparty.domain.Company;
import org.junit.jupiter.api.Test;

class NamingMappingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Company company = new Company("ABC123", "Acme Inc", "2021-05-01", "1 Main St", true);

    @Test
    void freeResponseUsesSnakeCase() throws Exception {
        String json = objectMapper.writeValueAsString(FreeThirdPartyController.FreeCompany.from(company));

        assertThat(json).contains("\"cin\":\"ABC123\"");
        assertThat(json).contains("\"registration_date\":\"2021-05-01\"");
        assertThat(json).contains("\"is_active\":true");
        assertThat(json).doesNotContain("registrationDate");
    }

    @Test
    void premiumResponseUsesCamelCaseAndMapsFullAddress() throws Exception {
        String json = objectMapper.writeValueAsString(PremiumThirdPartyController.PremiumCompany.from(company));

        assertThat(json).contains("\"companyIdentificationNumber\":\"ABC123\"");
        assertThat(json).contains("\"companyName\":\"Acme Inc\"");
        assertThat(json).contains("\"companyFullAddress\":\"1 Main St\"");
        assertThat(json).contains("\"isActive\":true");
        assertThat(json).doesNotContain("fullAddress\"");
    }
}
