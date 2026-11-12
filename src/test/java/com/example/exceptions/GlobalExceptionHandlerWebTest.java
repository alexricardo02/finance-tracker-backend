package com.example.exceptions;

import com.example.models.PaymentMethod;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Standalone MockMvc because no real endpoint can be made to throw these on demand.
class GlobalExceptionHandlerWebTest {

    private static final String SENSITIVE = "jdbc:postgresql://db.internal:5432/prod password=hunter2";

    static class Body {
        @NotNull(message = "The date is required")
        private LocalDate date;

        @NotNull(message = "Amount is required")
        private Double amount;

        private PaymentMethod paymentMethod;

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public PaymentMethod getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    }

    @RestController
    static class FailingController {
        @GetMapping("/runtime")
        void runtime() { throw new IllegalStateException(SENSITIVE); }

        @GetMapping("/checked")
        void checked() throws Exception { throw new java.io.IOException(SENSITIVE); }

        @GetMapping("/constraint")
        void constraint() { throw new DataIntegrityViolationException(SENSITIVE); }

        @GetMapping("/jpa-not-found")
        void jpaNotFound() { throw new EntityNotFoundException("Unable to find com.example.models.Income with id 42"); }

        @GetMapping("/library-illegal-argument")
        void libraryIllegalArgument() { throw new IllegalArgumentException(SENSITIVE); }

        @GetMapping("/security-exception")
        void securityException() { throw new SecurityException(SENSITIVE); }

        @GetMapping("/access-denied")
        void accessDenied() { throw new AccessDeniedException(SENSITIVE); }

        @GetMapping("/authentication-failure")
        void authenticationFailure() { throw new BadCredentialsException(SENSITIVE); }

        @GetMapping("/api/not-found")
        void apiNotFound() { throw new ResourceNotFoundException("Expense not found"); }

        @GetMapping("/api/bad-request")
        void apiBadRequest() { throw new BadRequestException("Invalid month"); }

        @GetMapping("/api/conflict")
        void apiConflict() { throw new ConflictException("Email already registered"); }

        @GetMapping("/api/invalid-credentials")
        void apiInvalidCredentials() { throw new InvalidCredentialsException("Invalid username or password"); }

        @GetMapping("/api/forbidden")
        void apiForbidden() { throw new ForbiddenResourceException("Not authorized to view this income"); }

        @GetMapping("/teapot")
        void status() { throw new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT, SENSITIVE); }

        @GetMapping("/unavailable")
        void unavailable() { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, SENSITIVE); }

        @GetMapping("/items/{id}")
        int item(@PathVariable int id) { return id; }

        @GetMapping("/totals")
        String totals(@RequestParam String month) { return month; }

        @GetMapping("/days")
        int days(@RequestParam @Min(value = 1, message = "Day must be at least 1")
                 @Max(value = 31, message = "Day must be at most 31") int day) { return day; }

        @PostMapping("/body")
        void body(@Valid @RequestBody Body body) { }

        @GetMapping("/constraint-violation")
        void constraintViolation() {
            throw new ConstraintViolationException(
                    Validation.buildDefaultValidatorFactory().getValidator().validate(new Body()));
        }
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new FailingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void runtimeException_returnsGeneric500WithoutInternals() throws Exception {
        assertGeneric(expectError(mvc.perform(get("/runtime")), 500, "An unexpected error occurred").andReturn());
    }

    @Test
    void checkedException_returnsGeneric500WithoutInternals() throws Exception {
        assertGeneric(expectError(mvc.perform(get("/checked")), 500, "An unexpected error occurred").andReturn());
    }

    @Test
    void dataIntegrityViolation_returns409WithoutSqlDetails() throws Exception {
        assertGeneric(expectError(mvc.perform(get("/constraint")), 409,
                "The operation violates a data constraint (invalid or duplicate reference)").andReturn());
    }

    @Test
    void jpaEntityNotFound_returns404WithoutEntityDetails() throws Exception {
        MvcResult result = expectError(mvc.perform(get("/jpa-not-found")), 404, "Resource not found").andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("com.example", "42");
    }

    @Test
    void libraryIllegalArgument_returns400WithGenericMessage() throws Exception {
        assertGeneric(expectError(mvc.perform(get("/library-illegal-argument")), 400, "Invalid request").andReturn());
    }

    @Test
    void securityException_fromLibrary_returnsGeneric500() throws Exception {
        assertGeneric(expectError(mvc.perform(get("/security-exception")), 500, "An unexpected error occurred").andReturn());
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "/api/not-found, 404, Expense not found",
            "/api/bad-request, 400, Invalid month",
            "/api/conflict, 409, Email already registered",
            "/api/invalid-credentials, 401, Invalid username or password",
            "/api/forbidden, 403, Not authorized to view this income"})
    void apiException_returnsItsStatusAndMessage(String path, int status, String message) throws Exception {
        expectError(mvc.perform(get(path)), status, message);
    }

    @Test
    void responseStatusException_keepsStatusWithGenericMessage() throws Exception {
        assertGeneric(expectError(mvc.perform(get("/teapot")), 418, "Request could not be processed").andReturn());
        assertGeneric(expectError(mvc.perform(get("/unavailable")), 503, "Service temporarily unavailable").andReturn());
    }

    @Test
    void ownershipFailure_returns403_whileBadCredentialsStay401() throws Exception {
        expectError(mvc.perform(get("/api/forbidden")), 403, "Not authorized to view this income");
        expectError(mvc.perform(get("/api/invalid-credentials")), 401, "Invalid username or password");
    }

    @Test
    void accessDenied_authenticatedUser_returns403() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("john", null, List.of()));

        assertGeneric(expectError(mvc.perform(get("/access-denied")), 403, "Access denied").andReturn());
    }

    @Test
    void accessDenied_anonymous_returns401() throws Exception {
        assertGeneric(expectError(mvc.perform(get("/access-denied")), 401, "Authentication required").andReturn());
    }

    @Test
    void authenticationException_returns401() throws Exception {
        assertGeneric(expectError(mvc.perform(get("/authentication-failure")), 401, "Authentication required").andReturn());
    }

    @Test
    void malformedJson_returns400() throws Exception {
        MvcResult result = expectError(mvc.perform(post("/body").contentType(MediaType.APPLICATION_JSON)
                .content("{not json")), 400, "Malformed JSON request").andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("not json", "Unexpected character");
    }

    @Test
    void invalidEnumValue_returns400NamingOnlyTheField() throws Exception {
        MvcResult result = expectError(mvc.perform(post("/body").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1,\"date\":\"2026-07-01\",\"paymentMethod\":\"BITCOIN\"}")),
                400, "Invalid value for field 'paymentMethod'").andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("BITCOIN", "com.example", "PaymentMethod");
    }

    @Test
    void invalidDateInBody_returns400NamingTheField() throws Exception {
        expectError(mvc.perform(post("/body").contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":1,\"date\":\"yesterday\"}")), 400, "Invalid value for field 'date'");
    }

    @Test
    void bodyValidation_reportsFirstFieldByNameDeterministically() throws Exception {
        for (int i = 0; i < 5; i++) {
            expectError(mvc.perform(post("/body").contentType(MediaType.APPLICATION_JSON).content("{}")),
                    400, "Amount is required");
        }
    }

    @Test
    void nonNumericPathVariable_returns400NamingTheParameter() throws Exception {
        expectError(mvc.perform(get("/items/abc")), 400, "Invalid value for parameter 'id'");
    }

    @Test
    void missingRequiredParameter_returns400NamingTheParameter() throws Exception {
        expectError(mvc.perform(get("/totals")), 400, "Missing required parameter 'month'");
    }

    @Test
    void parameterConstraintViolation_returns400WithConstraintMessage() throws Exception {
        expectError(mvc.perform(get("/days").param("day", "0")), 400, "Day must be at least 1");
    }

    @Test
    void constraintViolationException_returns400WithFirstViolationByPath() throws Exception {
        for (int i = 0; i < 5; i++) {
            expectError(mvc.perform(get("/constraint-violation")), 400, "Amount is required");
        }
    }

    @Test
    void unknownRoute_returns404() throws Exception {
        expectError(mvc.perform(get("/does-not-exist")), 404, "Resource not found");
    }

    @Test
    void unsupportedMethod_returns405WithAllowHeader() throws Exception {
        expectError(mvc.perform(delete("/items/1")), 405, "Method not allowed")
                .andExpect(header().string(HttpHeaders.ALLOW, "GET"));
    }

    private ResultActions expectError(ResultActions actions, int status, String message) throws Exception {
        return actions
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.httpCode").value(status))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.timeStamp").exists());
    }

    private void assertGeneric(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("hunter2", "jdbc:", "Exception", "\"trace\"");
    }
}
