package com.example.integration;

import com.example.models.Role;
import com.example.models.User;
import com.example.repository.CategoryRepository;
import com.example.repository.UserRepository;
import com.example.security.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractApiIntegrationTest extends AbstractIntegrationTest {

    protected static final String PASSWORD = "Passw0rd!";
    protected static final String TX_DATE = "2026-07-01";

    @Autowired protected TestRestTemplate rest;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected UserRepository userRepository;
    @Autowired protected CategoryRepository categoryRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected JwtUtil jwtUtil;

    protected record ApiUser(int id, String username, String email, String token) {
        String cookie() {
            return "auth_token=" + token;
        }
    }

    // Seeded directly because going through /register would consume the 3-per-hour IP bucket.
    protected ApiUser newUser() {
        return newUser("u" + UUID.randomUUID().toString().replace("-", ""));
    }

    protected ApiUser newUser(String username) {
        String email = UUID.randomUUID() + "@test.com";
        User user = new User(username, email, passwordEncoder.encode(PASSWORD), new Date());
        int id = userRepository.save(user).getUserId();
        return new ApiUser(id, username, email, jwtUtil.generateToken(username, Role.USER));
    }

    protected ResponseEntity<String> call(HttpMethod method, String path, ApiUser user, Object body) {
        return call(method, path, user, body, Map.of());
    }

    protected ResponseEntity<String> call(HttpMethod method, String path, ApiUser user, Object body,
                                          Map<String, String> extraHeaders) {
        HttpHeaders headers = new HttpHeaders();
        if (user != null) {
            headers.add(HttpHeaders.COOKIE, user.cookie());
        }
        extraHeaders.forEach(headers::add);
        return rest.exchange(path, method, new HttpEntity<>(toJson(body, headers), headers), String.class);
    }

    private String toJson(Object body, HttpHeaders headers) {
        if (body == null) {
            return null;
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            return body instanceof String raw ? raw : objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected JsonNode json(ResponseEntity<String> response) {
        try {
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new AssertionError("Body is not JSON: " + response.getBody(), e);
        }
    }

    protected int globalCategoryId(String name) {
        return categoryRepository.findByUserIsNull().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst().orElseThrow().getCategoryId();
    }

    protected int createCategory(ApiUser user, String name, String type) {
        ResponseEntity<String> response = call(HttpMethod.POST, "/api/categories", user,
                Map.of("name", name, "type", type));
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        return json(response).get("categoryId").asInt();
    }

    protected Map<String, Object> incomeBody(double amount, String currency, String date, String description) {
        return Map.of("amount", amount, "currency", currency, "date", date, "description", description,
                "categoryId", globalCategoryId("Salary"), "paymentMethod", "CASH");
    }

    protected Map<String, Object> expenseBody(double amount, String currency, String date, String description) {
        return Map.of("amount", amount, "currency", currency, "date", date, "description", description,
                "categoryId", globalCategoryId("Food"), "paymentMethod", "CASH");
    }

    protected int createIncome(ApiUser user, double amount, String description) {
        ResponseEntity<String> response = call(HttpMethod.POST, "/api/incomes", user,
                incomeBody(amount, "USD", TX_DATE, description));
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        return json(response).get("incomeId").asInt();
    }

    protected int createExpense(ApiUser user, double amount, String description) {
        ResponseEntity<String> response = call(HttpMethod.POST, "/api/expenses", user,
                expenseBody(amount, "USD", TX_DATE, description));
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        return json(response).get("id").asInt();
    }

    protected ResponseEntity<String> login(String username, String password) {
        return call(HttpMethod.POST, "/api/users/login", null, Map.of("username", username, "password", password));
    }

    protected static String cookieValue(ResponseEntity<String> response, String name) {
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).as("Set-Cookie headers").isNotNull();
        String header = cookies.stream().filter(c -> c.startsWith(name + "=")).findFirst().orElseThrow();
        int end = header.indexOf(';');
        return header.substring(name.length() + 1, end == -1 ? header.length() : end);
    }

    protected void assertErrorShape(ResponseEntity<String> response, int expectedStatus) {
        assertThat(response.getStatusCode().value()).as(response.getBody()).isEqualTo(expectedStatus);
        JsonNode body = json(response);
        assertThat(body.get("httpCode").asInt()).isEqualTo(expectedStatus);
        assertThat(body.get("message").asText()).isNotBlank();
        assertThat(body.hasNonNull("timeStamp")).isTrue();
        assertThat(response.getBody()).doesNotContain("Exception", "org.springframework", "java.", "SQL", "\"trace\"");
    }
}
