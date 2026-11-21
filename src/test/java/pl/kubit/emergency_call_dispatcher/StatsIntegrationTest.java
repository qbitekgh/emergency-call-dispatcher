package pl.kubit.emergency_call_dispatcher;

import org.junit.jupiter.api.Test;
import pl.kubit.emergency_call_dispatcher.call.Priority;

import java.time.Duration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/stats}: runs the hand-written SQL of {@code StatsRepository} against real PostgreSQL
 * ({@code GROUP BY}, {@code AVG} and {@code EXTRACT(EPOCH ...)} are not checked by unit tests with mocks).
 */
class StatsIntegrationTest extends IntegrationTest {

    @Test
    void shouldReturnZerosAndNoAverage_whenThereAreNoCalls() throws Exception {
        mockMvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCalls").value(0))
                .andExpect(jsonPath("$.byStatus.QUEUED").value(0))
                .andExpect(jsonPath("$.byPriority.CRITICAL").value(0))
                .andExpect(jsonPath("$.averageWaitSeconds").isEmpty());
    }

    @Test
    void shouldCountCallsAndAverageWaitTime_whenCallsWentThroughDifferentStatuses() throws Exception {
        // 10:00:00 - two calls arrive, nobody is available
        long waitedLong = receiveCall(Priority.CRITICAL);
        long abandoned = receiveCall(Priority.LOW);

        // 10:00:30 - a dispatcher logs in and takes the CRITICAL call after 30 s of waiting
        clock.advance(Duration.ofSeconds(30));
        availableDispatcher("Anna");
        mockMvc.perform(post("/api/calls/{id}/abandon", abandoned)).andExpect(status().isOk());

        // 10:01:00 - the call is closed; a new one arrives and is assigned immediately (0 s of waiting)
        clock.advance(Duration.ofSeconds(30));
        mockMvc.perform(post("/api/calls/{id}/close", waitedLong)).andExpect(status().isOk());
        receiveCall(Priority.CRITICAL);

        mockMvc.perform(get("/api/stats"))
                .andExpect(jsonPath("$.totalCalls").value(3))
                .andExpect(jsonPath("$.byStatus.QUEUED").value(0))
                .andExpect(jsonPath("$.byStatus.ASSIGNED").value(1))
                .andExpect(jsonPath("$.byStatus.CLOSED").value(1))
                .andExpect(jsonPath("$.byStatus.ABANDONED").value(1))
                .andExpect(jsonPath("$.byPriority.CRITICAL").value(2))
                .andExpect(jsonPath("$.byPriority.HIGH").value(0))
                .andExpect(jsonPath("$.byPriority.LOW").value(1))
                // (30 s + 0 s) / 2 calls that were assigned
                .andExpect(jsonPath("$.averageWaitSeconds").value(15.0));
    }
}
