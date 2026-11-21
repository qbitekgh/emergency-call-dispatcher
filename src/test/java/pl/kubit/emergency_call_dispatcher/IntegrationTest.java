package pl.kubit.emergency_call_dispatcher;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pl.kubit.emergency_call_dispatcher.call.Priority;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for integration tests: the whole application (controllers, services, JPA, JdbcTemplate, Flyway)
 * runs against a real PostgreSQL started by Testcontainers. Requests go through {@link MockMvc},
 * so they pass the same filters, validation and exception handler as real HTTP requests.
 * <p>
 * Every subclass uses the same configuration, so Spring starts the context and the container only once
 * and reuses them. Because of that, each test starts from empty tables and a reset clock.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, IntegrationTest.ClockConfiguration.class})
public abstract class IntegrationTest {

    protected static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected MutableClock clock;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @TestConfiguration(proxyBeanMethods = false)
    static class ClockConfiguration {

        /** {@code @Primary} wins over the production {@code Clock} bean wherever a {@code Clock} is injected. */
        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(T0);
        }
    }

    @BeforeEach
    void resetState() {
        // RESTART IDENTITY resets the id sequences, CASCADE is not needed: both tables are truncated together.
        jdbcTemplate.execute("TRUNCATE emergency_call, dispatcher RESTART IDENTITY");
        clock.setTo(T0);
    }

    /**
     * Registers a dispatcher and makes them AVAILABLE.
     *
     * @return the dispatcher's id
     */
    protected long availableDispatcher(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/dispatchers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        long id = idFrom(result);
        mockMvc.perform(put("/api/dispatchers/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "AVAILABLE"}
                                """))
                .andExpect(status().isOk());
        return id;
    }

    /**
     * Sends a valid call with the given priority.
     *
     * @return the call's id
     */
    protected long receiveCall(Priority priority) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/calls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "callerNumber": "+48123456789",
                                  "type": "MEDICAL",
                                  "priority": "%s",
                                  "location": "Main St 1"
                                }
                                """.formatted(priority)))
                .andExpect(status().isCreated())
                .andReturn();
        return idFrom(result);
    }

    private static long idFrom(MvcResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
