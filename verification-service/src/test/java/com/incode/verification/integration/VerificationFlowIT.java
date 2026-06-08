package com.incode.verification.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class VerificationFlowIT {

    private static final int PORT = findFreePort();
    private static final WireMockServer THIRD_PARTY =
            new WireMockServer(options().dynamicPort());

    @Container
    static final GenericContainer<?> DYNAMODB =
            new GenericContainer<>(DockerImageName.parse("amazon/dynamodb-local:latest")).withExposedPorts(8000);

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        THIRD_PARTY.start();
        registry.add("server.port", () -> PORT);
        registry.add("app.third-party.base-url", () -> "http://localhost:" + THIRD_PARTY.port());
        registry.add(
                "app.dynamodb.endpoint", () -> "http://" + DYNAMODB.getHost() + ":" + DYNAMODB.getMappedPort(8000));
        registry.add("app.dynamodb.region", () -> "us-east-1");
        registry.add("app.dynamodb.access-key", () -> "local");
        registry.add("app.dynamodb.secret-key", () -> "local");
        registry.add("app.persistence.create-table-on-startup", () -> true);
    }

    @AfterAll
    static void stopThirdParty() {
        THIRD_PARTY.stop();
    }

    @BeforeEach
    void resetStubs() {
        THIRD_PARTY.resetAll();
    }

    private void stubFree(int status, String body) {
        THIRD_PARTY.stubFor(get(urlPathEqualTo("/free-third-party"))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    private void stubPremium(int status, String body) {
        THIRD_PARTY.stubFor(get(urlPathEqualTo("/premium-third-party"))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    private String freeCompany(String cin) {
        return freeCompanies(cin);
    }

    private String freeCompanies(String... cins) {
        String companies = Arrays.stream(cins)
                .map(cin ->
                        """
                        {"cin":"%s","name":"Acme","registration_date":"2021-05-01","address":"1 Main St","is_active":true}"""
                                .formatted(cin))
                .collect(Collectors.joining(","));
        return "[" + companies + "]";
    }

    private String inactiveFreeCompany(String cin) {
        return """
                [{"cin":"%s","name":"Acme","registration_date":"2021-05-01","address":"1 Main St","is_active":false}]"""
                .formatted(cin);
    }

    private String premiumCompany(String cin) {
        return """
                [{"companyIdentificationNumber":"%s","companyName":"Acme","registrationDate":"2021-05-01",\
                "companyFullAddress":"1 Main St","isActive":true}]"""
                .formatted(cin);
    }

    private String base() {
        return "http://localhost:" + PORT;
    }

    private ResponseEntity<String> verify(String user, String pass, String id, String query) {
        return restTemplate
                .withBasicAuth(user, pass)
                .getForEntity(base() + "/backend-service?verificationId=" + id + "&query=" + query, String.class);
    }

    private JsonNode retrieve(String id) throws IOException {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("auditor", "auditor-pass")
                .getForEntity(base() + "/verifications/" + id, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode stored = objectMapper.readTree(response.getBody());
        assertThat(stored.get("verificationId").asText()).isEqualTo(id);
        assertThat(stored.hasNonNull("queryText")).isTrue();
        assertThat(stored.hasNonNull("timestamp")).isTrue();
        return stored;
    }

    @Test
    void persistsAndRetrievesUsingFreeSource() throws IOException {
        stubFree(200, freeCompany("CJQUNXGW"));
        String id = UUID.randomUUID().toString();

        ResponseEntity<String> response = verify("verifier", "verifier-pass", id, "CJ");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode created = objectMapper.readTree(response.getBody());
        assertThat(created.get("verificationId").asText()).isEqualTo(id);
        assertThat(created.get("query").asText()).isEqualTo("CJ");

        JsonNode stored = retrieve(id);
        assertThat(stored.get("queryText").asText()).isEqualTo("CJ");
        assertThat(stored.get("source").asText()).isEqualTo("FREE");
        assertThat(stored.get("result").get("cin").asText()).isEqualTo("CJQUNXGW");
    }

    @Test
    void fallsBackToPremiumWhenFreeReturns503() throws IOException {
        stubFree(503, "");
        stubPremium(200, premiumCompany("CJQUNXGW"));
        String id = UUID.randomUUID().toString();

        verify("verifier", "verifier-pass", id, "CJ");

        assertThat(retrieve(id).get("source").asText()).isEqualTo("PREMIUM");
    }

    @Test
    void fallsBackToPremiumWhenFreeIsEmpty() throws IOException {
        stubFree(200, "[]");
        stubPremium(200, premiumCompany("LDL93LOZ"));
        String id = UUID.randomUUID().toString();

        verify("verifier", "verifier-pass", id, "LDL93LOZ");

        JsonNode stored = retrieve(id);
        assertThat(stored.get("source").asText()).isEqualTo("PREMIUM");
        assertThat(stored.get("result").get("cin").asText()).isEqualTo("LDL93LOZ");
    }

    @Test
    void reportsThirdPartiesDownWhenBothUnavailable() throws IOException {
        stubFree(503, "");
        stubPremium(503, "");
        String id = UUID.randomUUID().toString();

        verify("verifier", "verifier-pass", id, "CJ");

        JsonNode stored = retrieve(id);
        assertThat(stored.get("result").get("status").asText()).isEqualTo("THIRD_PARTIES_DOWN");
    }

    @Test
    void reportsThirdPartiesDownWhenFreeIsEmptyAndPremiumUnavailable() throws IOException {
        stubFree(200, "[]");
        stubPremium(503, "");
        String id = UUID.randomUUID().toString();

        verify("verifier", "verifier-pass", id, "CJ");

        JsonNode stored = retrieve(id);
        assertThat(stored.get("result").get("status").asText()).isEqualTo("THIRD_PARTIES_DOWN");
    }

    @Test
    void rejectsDuplicateVerificationId() throws IOException {
        stubFree(200, freeCompany("CJQUNXGW"));
        String id = UUID.randomUUID().toString();

        verify("verifier", "verifier-pass", id, "CJ");
        ResponseEntity<String> duplicate = verify("verifier", "verifier-pass", id, "CJ");

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        JsonNode body = objectMapper.readTree(duplicate.getBody());
        assertThat(body.get("status").asInt()).isEqualTo(409);
        assertThat(body.get("error").asText()).isEqualTo("Conflict");
    }

    @Test
    void rejectsMissingCredentialsAndWrongRole() {
        String id = UUID.randomUUID().toString();

        ResponseEntity<String> anonymous =
                restTemplate.getForEntity(base() + "/backend-service?verificationId=" + id + "&query=CJ", String.class);
        ResponseEntity<String> wrongRole = verify("auditor", "auditor-pass", id, "CJ");

        assertThat(anonymous.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wrongRole.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void propagatesTraceContextToThirdParty() {
        stubFree(200, freeCompany("CJQUNXGW"));
        String id = UUID.randomUUID().toString();

        verify("verifier", "verifier-pass", id, "CJ");

        THIRD_PARTY.verify(
                getRequestedFor(urlPathEqualTo("/free-third-party")).withHeader("traceparent", matching(".+")));
    }

    @Test
    void rejectsInvalidVerificationId() throws IOException {
        ResponseEntity<String> response = verify("verifier", "verifier-pass", "not-a-guid", "CJ");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(objectMapper.readTree(response.getBody()).get("status").asInt())
                .isEqualTo(400);
    }

    @Test
    void rejectsBlankQuery() {
        String id = UUID.randomUUID().toString();

        ResponseEntity<String> response = verify("verifier", "verifier-pass", id, "");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void returnsNotFoundForUnknownVerification() throws IOException {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("auditor", "auditor-pass")
                .getForEntity(base() + "/verifications/" + UUID.randomUUID(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(objectMapper.readTree(response.getBody()).get("status").asInt())
                .isEqualTo(404);
    }

    @Test
    void returnsOtherResultsForMultipleActiveMatches() throws IOException {
        stubFree(200, freeCompanies("CJQUNXGW", "FI75L0O9"));
        String id = UUID.randomUUID().toString();

        verify("verifier", "verifier-pass", id, "Q");

        JsonNode stored = retrieve(id);
        assertThat(stored.get("result").get("cin").asText()).isEqualTo("CJQUNXGW");
        JsonNode otherResults = stored.get("otherResults");
        assertThat(otherResults.isArray()).isTrue();
        assertThat(otherResults).isNotEmpty();
        otherResults.forEach(
                company -> assertThat(company.get("active").asBoolean()).isTrue());
    }

    @Test
    void returnsNoResultsWhenOnlyInactive() throws IOException {
        stubFree(200, inactiveFreeCompany("0ANW1LCD"));
        String id = UUID.randomUUID().toString();

        verify("verifier", "verifier-pass", id, "0ANW1LCD");

        assertThat(retrieve(id).get("result").get("status").asText()).isEqualTo("NO_RESULTS");
    }

    @Test
    void rejectsVerifierReadingVerifications() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("verifier", "verifier-pass")
                .getForEntity(base() + "/verifications/" + UUID.randomUUID(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void rejectsMissingQueryParameter() throws IOException {
        String id = UUID.randomUUID().toString();

        ResponseEntity<String> response = restTemplate
                .withBasicAuth("verifier", "verifier-pass")
                .getForEntity(base() + "/backend-service?verificationId=" + id, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(objectMapper.readTree(response.getBody()).get("status").asInt())
                .isEqualTo(400);
    }

    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException exception) {
            throw new IllegalStateException("No free port available", exception);
        }
    }
}
