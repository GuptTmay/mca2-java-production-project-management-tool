package com.taskflow.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * This is the one test in the suite that proves the application actually
 * boots and wires together end to end: real Spring context, real Spring
 * Security filter chain, real JPA/Hibernate against a real PostgreSQL
 * instance (via Testcontainers), with the schema built exclusively by
 * running the Flyway migrations - not
 * hibernate.ddl-auto. Everything else in this suite is a mocked unit test;
 * this is the one that can't lie about whether the pieces fit together.
 *
 * Requires a Docker daemon (Testcontainers) and is skipped when one is unavailable.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@EnabledIf("dockerAvailable")
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("taskflow_test")
            .withUsername("taskflow")
            .withPassword("taskflow");

    static boolean dockerAvailable() {
        return DockerClientFactory.instance().isDockerAvailable();
    }

    @DynamicPropertySource
    static void overrideDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Bypasses the JWT_SECRET env var requirement for this test run only.
        registry.add("taskflow.jwt.secret", () -> "integration-test-only-secret-key-32-bytes-min!!");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerThenLogin_returnsWorkingJwtTokens() throws Exception {
        Map<String, String> registerRequest = Map.of(
                "email", "integration@taskflow.io",
                "password", "password123",
                "firstName", "Integration",
                "lastName", "Test",
                "role", "TEAM_MEMBER");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("integration@taskflow.io"))
                .andExpect(jsonPath("$.user.role").value("TEAM_MEMBER"));

        Map<String, String> loginRequest = Map.of(
                "email", "integration@taskflow.io",
                "password", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void register_rejectsDuplicateEmailWithConflict() throws Exception {
        Map<String, String> request = Map.of(
                "email", "duplicate@taskflow.io",
                "password", "password123",
                "firstName", "A",
                "lastName", "B",
                "role", "CONTENT_MANAGER");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void login_rejectsWrongPasswordWithUnauthorized() throws Exception {
        Map<String, String> registerRequest = Map.of(
                "email", "wrongpass@taskflow.io",
                "password", "password123",
                "firstName", "A",
                "lastName", "B",
                "role", "TEAM_MEMBER");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        Map<String, String> badLogin = Map.of("email", "wrongpass@taskflow.io", "password", "not-the-password");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_rejectsRequestWithNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
