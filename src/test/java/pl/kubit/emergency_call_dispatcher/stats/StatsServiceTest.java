package pl.kubit.emergency_call_dispatcher.stats;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.kubit.emergency_call_dispatcher.call.CallStatus;
import pl.kubit.emergency_call_dispatcher.call.Priority;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private StatsRepository statsRepository;

    @InjectMocks
    private StatsService statsService;

    @Test
    void shouldSumAllStatusesAsTotal_whenStatsAreRequested() {
        Map<CallStatus, Long> byStatus = new EnumMap<>(Map.of(
                CallStatus.QUEUED, 2L, CallStatus.ASSIGNED, 1L, CallStatus.CLOSED, 4L, CallStatus.ABANDONED, 0L));
        Map<Priority, Long> byPriority = new EnumMap<>(Map.of(
                Priority.CRITICAL, 3L, Priority.HIGH, 0L, Priority.MEDIUM, 4L, Priority.LOW, 0L));
        when(statsRepository.countByStatus()).thenReturn(byStatus);
        when(statsRepository.countByPriority()).thenReturn(byPriority);
        when(statsRepository.averageWaitSeconds()).thenReturn(42.5);

        CallStats stats = statsService.getStats();

        assertThat(stats.totalCalls()).isEqualTo(7);
        assertThat(stats.byStatus()).isSameAs(byStatus);
        assertThat(stats.byPriority()).isSameAs(byPriority);
        assertThat(stats.averageWaitSeconds()).isEqualTo(42.5);
    }

    @Test
    void shouldReturnZeroTotalAndNoAverage_whenThereAreNoCalls() {
        Map<CallStatus, Long> byStatus = new EnumMap<>(CallStatus.class);
        for (CallStatus status : CallStatus.values()) {
            byStatus.put(status, 0L);
        }
        when(statsRepository.countByStatus()).thenReturn(byStatus);
        when(statsRepository.countByPriority()).thenReturn(new EnumMap<>(Priority.class));
        when(statsRepository.averageWaitSeconds()).thenReturn(null);

        CallStats stats = statsService.getStats();

        assertThat(stats.totalCalls()).isZero();
        assertThat(stats.averageWaitSeconds()).isNull();
    }
}
