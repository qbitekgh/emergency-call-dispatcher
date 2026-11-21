package pl.kubit.emergency_call_dispatcher.dispatch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import pl.kubit.emergency_call_dispatcher.call.CallType;
import pl.kubit.emergency_call_dispatcher.call.EmergencyCall;
import pl.kubit.emergency_call_dispatcher.call.Priority;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QueueController.class)
class QueueControllerTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DispatchService dispatchService;

    @Test
    void shouldReturnQueuedCallsGroupedByPriority_whenQueueIsRequested() throws Exception {
        when(dispatchService.queue()).thenReturn(List.of(call(2L, Priority.CRITICAL), call(1L, Priority.LOW)));

        mockMvc.perform(get("/api/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.byPriority.CRITICAL[0].id").value(2))
                .andExpect(jsonPath("$.byPriority.LOW[0].id").value(1))
                .andExpect(jsonPath("$.byPriority.HIGH").doesNotExist());
    }

    private static EmergencyCall call(Long id, Priority priority) {
        EmergencyCall call = new EmergencyCall("+48123456789", CallType.FIRE, priority, "Main St 1", null, T0);
        ReflectionTestUtils.setField(call, "id", id);
        return call;
    }
}
