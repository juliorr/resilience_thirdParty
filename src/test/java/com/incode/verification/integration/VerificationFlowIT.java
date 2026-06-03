package com.incode.verification.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.incode.verification.mock.FailureSimulator;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class VerificationFlowIT {

    private static final int PORT = findFreePort();
    private static final double FREE_PROBABILITY = 0.40;
    private static final double PREMIUM_PROBABILITY = 0.10;

    @Container
    static final GenericContainer<?> DYNAMODB =
            new GenericContainer<>(DockerImageName.parse("amazon/dynamodb-local:latest")).withExposedPorts(8000);

    @MockitoBean
    private FailureSimulator failureSimulator;

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("server.port", () -> PORT);
        registry.add("app.third-party.base-url", () -> "http://localhost:" + PORT);
        registry.add(
                "app.dynamodb.endpoint", () -> "http://" + DYNAMODB.getHost() + ":" + DYNAMODB.getMappedPort(8000));
        registry.add("app.dynamodb.region", () -> "us-east-1");
        registry.add("app.dynamodb.access-key", () -> "local");
        registry.add("app.dynamodb.secret-key", () -> "local");
        registry.add("app.persistence.create-table-on-startup", () -> true);
    }

    @BeforeEach
    void resetSimulator() {
        Mockito.when(failureSimulator.shouldFail(Mockito.anyDouble())).thenReturn(false);
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
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.getBody());
    }

    @Test
    void persistsAndRetrievesUsingFreeSource() throws IOException {
        String id = UUID.randomUUID().toString();

        ResponseEntity<String> response = verify("verifier", "verifier-pass", id, "CJ");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode stored = retrieve(id);
        assertThat(stored.get("source").asText()).isEqualTo("FREE");
        assertThat(stored.get("result").get("cin").asText()).isEqualTo("CJQUNXGW");
    }

    @Test
    void fallsBackToPremiumWhenFreeReturns503() throws IOException {
        Mockito.when(failureSimulator.shouldFail(FREE_PROBABILITY)).thenReturn(true);
        Mockito.when(failureSimulator.shouldFail(PREMIUM_PROBABILITY)).thenReturn(false);
        String id = UUID.randomUUID().toString();

        verify("verifier", "verifier-pass", id, "CJ");

        assertThat(retrieve(id).get("source").asText()).isEqualTo("PREMIUM");
    }

    @Test
    void fallsBackToPremiumWhenFreeIsEmpty() throws IOException {
        String id = UUID.randomUUID().toString();

        verify("verifier", "verifier-pass", id, "LDL93LOZ");

        JsonNode stored = retrieve(id);
        assertThat(stored.get("source").asText()).isEqualTo("PREMIUM");
        assertThat(stored.get("result").get("cin").asText()).isEqualTo("LDL93LOZ");
    }

    @Test
    void reportsThirdPartiesDownWhenBothUnavailable() throws IOException {
        Mockito.when(failureSimulator.shouldFail(Mockito.anyDouble())).thenReturn(true);
        String id = UUID.randomUUID().toString();

        verify("verifier", "verifier-pass", id, "CJ");

        JsonNode stored = retrieve(id);
        assertThat(stored.get("result").get("status").asText()).isEqualTo("THIRD_PARTIES_DOWN");
    }

    @Test
    void rejectsDuplicateVerificationId() {
        String id = UUID.randomUUID().toString();

        verify("verifier", "verifier-pass", id, "CJ");
        ResponseEntity<String> duplicate = verify("verifier", "verifier-pass", id, "CJ");

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
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
    void returnsNotFoundForUnknownVerification() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("auditor", "auditor-pass")
                .getForEntity(base() + "/verifications/" + UUID.randomUUID(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException exception) {
            throw new IllegalStateException("No free port available", exception);
        }
    }
}
