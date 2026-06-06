package com.incode.thirdparty.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.thirdparty.domain.Company;
import com.incode.thirdparty.domain.Source;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompanyDataStoreTest {

    private final CompanyDataStore store = new CompanyDataStore(new ObjectMapper());

    @Test
    void filtersBySubstringOnCinCaseInsensitive() {
        List<Company> lower = store.search(Source.FREE, "cjqunxgw");
        List<Company> upper = store.search(Source.FREE, "CJ");

        assertThat(lower).extracting(Company::cin).contains("CJQUNXGW");
        assertThat(upper).extracting(Company::cin).contains("CJQUNXGW");
    }

    @Test
    void matchesCinNotName() {
        List<Company> byName = store.search(Source.FREE, "Ramirez");

        assertThat(byName).isEmpty();
    }

    @Test
    void premiumIsSupersetWithEntriesAbsentFromFree() {
        List<Company> inPremium = store.search(Source.PREMIUM, "LDL93LOZ");
        List<Company> inFree = store.search(Source.FREE, "LDL93LOZ");

        assertThat(inPremium).extracting(Company::cin).contains("LDL93LOZ");
        assertThat(inFree).isEmpty();
    }

    @Test
    void mapsPremiumFullAddressIntoCanonicalAddress() {
        Company company = store.search(Source.PREMIUM, "LDL93LOZ").getFirst();

        assertThat(company.address()).isNotBlank();
        assertThat(company.name()).isNotBlank();
        assertThat(company.registrationDate()).isNotBlank();
    }
}
