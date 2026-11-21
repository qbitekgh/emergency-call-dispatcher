package pl.kubit.emergency_call_dispatcher.stats;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.kubit.emergency_call_dispatcher.call.CallStatus;
import pl.kubit.emergency_call_dispatcher.call.Priority;

import java.util.EnumMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatsController.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatsService statsService;

    @Test
    void shouldReturnCountsAndAverageWait_whenStatsAreRequested() throws Exception {
        when(statsService.getStats()).thenReturn(new CallStats(
                3,
                new EnumMap<>(Map.of(CallStatus.QUEUED, 1L, CallStatus.CLOSED, 2L)),
                new EnumMap<>(Map.of(Priority.CRITICAL, 3L)),
                12.5));

        mockMvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCalls").value(3))
                .andExpect(jsonPath("$.byStatus.QUEUED").value(1))
                .andExpect(jsonPath("$.byStatus.CLOSED").value(2))
                .andExpect(jsonPath("$.byPriority.CRITICAL").value(3))
                .andExpect(jsonPath("$.averageWaitSeconds").value(12.5));
    }
}
