package pl.kubit.emergency_call_dispatcher;

import org.junit.jupiter.api.Test;
import pl.kubit.emergency_call_dispatcher.call.Priority;

import java.time.Duration;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * {@code GET /api/queue} with the default "priority" strategy, reading calls from the real database.
 */
class QueueIntegrationTest extends IntegrationTest {

    @Test
    void shouldGroupQueuedCallsByPriorityAndKeepArrivalOrder_whenNoDispatcherIsAvailable() throws Exception {
        long low = receiveCall(Priority.LOW);
        clock.advance(Duration.ofSeconds(1));
        long firstCritical = receiveCall(Priority.CRITICAL);
        clock.advance(Duration.ofSeconds(1));
        long high = receiveCall(Priority.HIGH);
        clock.advance(Duration.ofSeconds(1));
        long secondCritical = receiveCall(Priority.CRITICAL);

        mockMvc.perform(get("/api/queue"))
                .andExpect(jsonPath("$.size").value(4))
                .andExpect(jsonPath("$.byPriority.CRITICAL[*].id", contains((int) firstCritical, (int) secondCritical)))
                .andExpect(jsonPath("$.byPriority.HIGH[*].id", contains((int) high)))
                .andExpect(jsonPath("$.byPriority.LOW[*].id", contains((int) low)))
                .andExpect(jsonPath("$.byPriority.MEDIUM").doesNotExist());
    }

    @Test
    void shouldAssignMostUrgentCallFirst_whenDispatcherBecomesAvailable() throws Exception {
        receiveCall(Priority.LOW);
        clock.advance(Duration.ofSeconds(1));
        long critical = receiveCall(Priority.CRITICAL);

        long dispatcherId = availableDispatcher("Anna");

        mockMvc.perform(get("/api/calls/{id}", critical))
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.assignedDispatcherId").value(dispatcherId));
        mockMvc.perform(get("/api/queue"))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.byPriority.LOW", hasSize(1)));
    }
}
