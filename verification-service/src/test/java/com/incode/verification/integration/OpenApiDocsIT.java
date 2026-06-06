package com.incode.verification.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.ServerSocket;
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
class OpenApiDocsIT {

    private static final int PORT = findFreePort();

    @Container
    static final GenericContainer<?> DYNAMODB =
            new GenericContainer<>(DockerImageName.parse("amazon/dynamodb-local:latest")).withExposedPorts(8000);

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("server.port", () -> PORT);
        registry.add("app.third-party.base-url", () -> "http://localhost:1");
        registry.add(
                "app.dynamodb.endpoint", () -> "http://" + DYNAMODB.getHost() + ":" + DYNAMODB.getMappedPort(8000));
        registry.add("app.dynamodb.region", () -> "us-east-1");
        registry.add("app.dynamodb.access-key", () -> "local");
        registry.add("app.dynamodb.secret-key", () -> "local");
        registry.add("app.persistence.create-table-on-startup", () -> true);
    }

    @Test
    void documentsErrorResponsesForBackendService() throws IOException {
        JsonNode responses =
                apiDocs().get("paths").get("/backend-service").get("get").get("responses");

        assertThat(responses.has("200")).isTrue();
        assertThat(responses.has("400")).isTrue();
        assertThat(responses.has("401")).isTrue();
        assertThat(responses.has("403")).isTrue();
        assertThat(responses.has("409")).isTrue();
        assertThat(responses.has("500")).isTrue();
        assertThat(errorSchemaRef(responses.get("400"))).isEqualTo("#/components/schemas/ApiError");
    }

    @Test
    void documentsErrorResponsesForVerificationRetrieval() throws IOException {
        JsonNode responses = apiDocs()
                .get("paths")
                .get("/verifications/{verificationId}")
                .get("get")
                .get("responses");

        assertThat(responses.has("200")).isTrue();
        assertThat(responses.has("400")).isTrue();
        assertThat(responses.has("401")).isTrue();
        assertThat(responses.has("403")).isTrue();
        assertThat(responses.has("404")).isTrue();
        assertThat(responses.has("500")).isTrue();
        assertThat(errorSchemaRef(responses.get("404"))).isEqualTo("#/components/schemas/ApiError");
    }

    private JsonNode apiDocs() throws IOException {
        ResponseEntity<String> response =
                restTemplate.getForEntity("http://localhost:" + PORT + "/v3/api-docs", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return new ObjectMapper().readTree(response.getBody());
    }

    private String errorSchemaRef(JsonNode response) {
        return response.get("content")
                .get("application/json")
                .get("schema")
                .get("$ref")
                .asText();
    }

    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException exception) {
            throw new IllegalStateException("No free port available", exception);
        }
    }
}
