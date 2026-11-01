package se.cloudneutral.orders.api;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import se.cloudneutral.orders.config.JacksonConfig;
import se.cloudneutral.orders.service.BusinessException;
import se.cloudneutral.orders.service.IdempotencyException;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit-test")
class RestExceptionHandlerTest {
    @RestController
    static class FailingController {
        @PostMapping("/orders")
        void place(@RequestHeader(OrderController.HEADER_IDEMPOTENCY_KEY) String key) {
        }

        @PostMapping("/other")
        void other(@RequestHeader("X-Other") String other) {
        }

        @GetMapping("/reused")
        void reused() {
            throw new IdempotencyException(IdempotencyException.Reason.FINGERPRINT_MISMATCH, "reused");
        }

        @GetMapping("/lost")
        void lost() {
            throw new IdempotencyException(IdempotencyException.Reason.RECORD_LOST, "lost");
        }

        @GetMapping("/rejected")
        void rejected() {
            throw new BusinessException("Constraint violation in order placement", null);
        }

        @GetMapping("/unknown")
        void unknown() {
            throw NoSuchResourceException.product(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        }

        @GetMapping("/not-found")
        void notFound() {
            throw new ResponseStatusException(NOT_FOUND, "No such order");
        }

        @GetMapping("/broken")
        void broken() {
            throw new IllegalStateException("secret internals");
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new RestExceptionHandler())
                // The application's mapper, so the problem detail mixin is exercised too
                .setMessageConverters(new JacksonJsonHttpMessageConverter(new JacksonConfig().primaryObjectMapper()))
                .build();
    }

    @Test
    void givenMissingIdempotencyKey_thenExpectTypedProblem() throws Exception {
        mockMvc.perform(post("/orders"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value(ProblemTypes.IDEMPOTENCY_KEY_REQUIRED.toString()))
                .andExpect(jsonPath("$.title").value("Idempotency key required"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.properties").doesNotExist());
    }

    @Test
    void givenOtherMissingHeader_thenExpectUntypedProblem() throws Exception {
        mockMvc.perform(post("/other"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").doesNotExist())
                .andExpect(jsonPath("$.detail").value("Required header 'X-Other' is not present."));
    }

    @Test
    void givenIdempotencyFailures_thenExpectTypedProblems() throws Exception {
        mockMvc.perform(get("/reused"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.type").value(ProblemTypes.IDEMPOTENCY_KEY_REUSED.toString()));

        mockMvc.perform(get("/lost"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ProblemTypes.IDEMPOTENCY_RECORD_LOST.toString()));
    }

    @Test
    void givenDomainFailures_thenExpectTypedProblems() throws Exception {
        mockMvc.perform(get("/rejected"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ProblemTypes.ORDER_REJECTED.toString()))
                .andExpect(jsonPath("$.title").value("Order rejected"));

        mockMvc.perform(get("/unknown"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.type").value(ProblemTypes.UNKNOWN_REFERENCE.toString()))
                .andExpect(jsonPath("$.detail").value("No such product: 00000000-0000-0000-0000-000000000000"));
    }

    @Test
    void givenPlainStatus_thenExpectProblemWithoutType() throws Exception {
        mockMvc.perform(get("/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").doesNotExist())
                .andExpect(jsonPath("$.detail").value("No such order"));
    }

    @Test
    void givenUnexpectedFailure_thenExpectProblemWithoutDetails() throws Exception {
        mockMvc.perform(get("/broken"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").doesNotExist());
    }
}
