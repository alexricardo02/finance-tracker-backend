package com.example.exceptions;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Standalone MockMvc because no real endpoint can be made to throw these on demand.
class GlobalExceptionHandlerWebTest {

    private static final String SENSITIVE = "jdbc:postgresql://db.internal:5432/prod password=hunter2";

    @RestController
    static class FailingController {
        @GetMapping("/runtime")
        void runtime() { throw new IllegalStateException(SENSITIVE); }

        @GetMapping("/checked")
        void checked() throws Exception { throw new java.io.IOException(SENSITIVE); }

        @GetMapping("/constraint")
        void constraint() { throw new DataIntegrityViolationException(SENSITIVE); }

        @GetMapping("/not-found")
        void notFound() { throw new EntityNotFoundException("Income not found"); }

        @GetMapping("/forbidden")
        void forbidden() { throw new ForbiddenResourceException("Not yours"); }

        @GetMapping("/bad-credentials")
        void badCredentials() { throw new SecurityException("Bad credentials"); }

        @GetMapping("/teapot")
        void status() { throw new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT, "teapot"); }
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new FailingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void runtimeException_returnsGeneric500WithoutInternals() throws Exception {
        assertGeneric(mvc.perform(get("/runtime")).andExpect(status().isInternalServerError()).andReturn(),
                "An unexpected error occurred");
    }

    @Test
    void checkedException_returnsGeneric500WithoutInternals() throws Exception {
        assertGeneric(mvc.perform(get("/checked")).andExpect(status().isInternalServerError()).andReturn(),
                "An unexpected error occurred");
    }

    @Test
    void dataIntegrityViolation_returns409WithoutSqlDetails() throws Exception {
        assertGeneric(mvc.perform(get("/constraint")).andExpect(status().isConflict()).andReturn(),
                "The operation violates a data constraint (invalid or duplicate reference)");
    }

    @Test
    void entityNotFound_returns404WithErrorShape() throws Exception {
        mvc.perform(get("/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.httpCode").value(404))
                .andExpect(jsonPath("$.message").value("Income not found"))
                .andExpect(jsonPath("$.timeStamp").exists());
    }

    @Test
    void responseStatusException_keepsStatusAndReason() throws Exception {
        mvc.perform(get("/teapot"))
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message").value("teapot"));
    }

    @Test
    void ownershipFailure_returns403_whileBadCredentialsStay401() throws Exception {
        mvc.perform(get("/forbidden")).andExpect(status().isForbidden()).andExpect(jsonPath("$.httpCode").value(403));
        mvc.perform(get("/bad-credentials")).andExpect(status().isUnauthorized());
    }

    private void assertGeneric(MvcResult result, String expectedMessage) throws Exception {
        String body = result.getResponse().getContentAsString();
        assertThat(body).contains(expectedMessage).doesNotContain("hunter2", "jdbc:", "Exception", "\"trace\"");
    }
}
