package pl.kubit.emergency_call_dispatcher;

import org.junit.jupiter.api.Test;
import pl.kubit.emergency_call_dispatcher.call.Priority;

import java.time.Duration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full dispatching cycle through the REST API, with every layer and a real database.
 */
class DispatchFlowIntegrationTest extends IntegrationTest {

    @Test
    void shouldAssignQueuedCallToDispatcher_whenDispatcherClosesPreviousCall() throws Exception {
        long dispatcherId = availableDispatcher("Anna");

        // 1. The first call is assigned right away: a dispatcher is AVAILABLE.
        long firstCallId = receiveCall(Priority.HIGH);
        mockMvc.perform(get("/api/calls/{id}", firstCallId))
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.assignedDispatcherId").value(dispatcherId))
                .andExpect(jsonPath("$.assignedAt").value("2026-01-01T10:00:00Z"));
        mockMvc.perform(get("/api/dispatchers"))
                .andExpect(jsonPath("$[0].status").value("BUSY"));

        // 2. The second call waits: the only dispatcher is BUSY.
        clock.advance(Duration.ofSeconds(10));
        long secondCallId = receiveCall(Priority.MEDIUM);
        mockMvc.perform(get("/api/calls/{id}", secondCallId))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.assignedDispatcherId").isEmpty());
        mockMvc.perform(get("/api/queue"))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.byPriority.MEDIUM[0].id").value(secondCallId));

        // 3. Closing the first call frees the dispatcher, who immediately gets the queued call.
        clock.advance(Duration.ofSeconds(50));
        mockMvc.perform(post("/api/calls/{id}/close", firstCallId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closedAt").value("2026-01-01T10:01:00Z"));
        mockMvc.perform(get("/api/calls/{id}", secondCallId))
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.assignedDispatcherId").value(dispatcherId))
                .andExpect(jsonPath("$.assignedAt").value("2026-01-01T10:01:00Z"));
        mockMvc.perform(get("/api/dispatchers"))
                .andExpect(jsonPath("$[0].status").value("BUSY"));
        mockMvc.perform(get("/api/queue"))
                .andExpect(jsonPath("$.size").value(0));
    }

    @Test
    void shouldGiveCallToDispatcherWaitingLongest_whenSeveralAreAvailable() throws Exception {
        long waitingLongerId = availableDispatcher("Anna");
        clock.advance(Duration.ofMinutes(1));
        availableDispatcher("Bartek");

        long callId = receiveCall(Priority.LOW);

        mockMvc.perform(get("/api/calls/{id}", callId))
                .andExpect(jsonPath("$.assignedDispatcherId").value(waitingLongerId));
    }

    @Test
    void shouldReturn409AndKeepCallUnchanged_whenClosingQueuedCall() throws Exception {
        long callId = receiveCall(Priority.HIGH);

        mockMvc.perform(post("/api/calls/{id}/close", callId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Illegal status transition"));
        mockMvc.perform(get("/api/calls").param("status", "QUEUED"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(callId));
    }
}
