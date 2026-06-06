package com.incode.thirdparty.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.thirdparty.domain.Company;
import com.incode.thirdparty.domain.Source;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class CompanyDataStore {

    private static final String FREE_RESOURCE = "data/free_service_companies-1.json";
    private static final String PREMIUM_RESOURCE = "data/premium_service_companies-1.json";

    private final Map<Source, List<Company>> datasets;

    public CompanyDataStore(ObjectMapper objectMapper) {
        this.datasets = Map.of(
                Source.FREE, loadFree(objectMapper),
                Source.PREMIUM, loadPremium(objectMapper));
    }

    public List<Company> search(Source source, String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        return datasets.get(source).stream()
                .filter(company -> company.cin().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    private List<Company> loadFree(ObjectMapper objectMapper) {
        return read(objectMapper, FREE_RESOURCE, FreeRecord[].class).stream()
                .map(FreeRecord::toCompany)
                .toList();
    }

    private List<Company> loadPremium(ObjectMapper objectMapper) {
        return read(objectMapper, PREMIUM_RESOURCE, PremiumRecord[].class).stream()
                .map(PremiumRecord::toCompany)
                .toList();
    }

    private <T> List<T> read(ObjectMapper objectMapper, String resource, Class<T[]> type) {
        try (InputStream input = new ClassPathResource(resource).getInputStream()) {
            return List.of(objectMapper.readValue(input, type));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load seed data from " + resource, exception);
        }
    }

    private record FreeRecord(
            String cin,
            String name,
            @JsonProperty("registration_date") String registrationDate,
            String address,
            @JsonProperty("is_active") boolean active) {

        Company toCompany() {
            return new Company(cin, name, registrationDate, address, active);
        }
    }

    private record PremiumRecord(
            @JsonProperty("companyIdentificationNumber") String cin,
            @JsonProperty("companyName") String name,
            @JsonProperty("registrationDate") String registrationDate,
            @JsonProperty("fullAddress") String address,
            @JsonProperty("isActive") boolean active) {

        Company toCompany() {
            return new Company(cin, name, registrationDate, address, active);
        }
    }
}
