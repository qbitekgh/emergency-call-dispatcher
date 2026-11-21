package pl.kubit.emergency_call_dispatcher.dispatch;

import org.junit.jupiter.api.Test;
import pl.kubit.emergency_call_dispatcher.call.CallType;
import pl.kubit.emergency_call_dispatcher.call.EmergencyCall;
import pl.kubit.emergency_call_dispatcher.call.Priority;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FifoStrategyTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    private final QueueOrderingStrategy strategy = new FifoStrategy();

    @Test
    void shouldOrderByReceivedTimeOnly_whenPrioritiesDiffer() {
        EmergencyCall first = call(Priority.LOW, T0);
        EmergencyCall second = call(Priority.CRITICAL, T0.plusSeconds(10));
        EmergencyCall third = call(Priority.MEDIUM, T0.plusSeconds(20));

        List<EmergencyCall> ordered = List.of(third, second, first).stream()
                .sorted(strategy.queueOrder())
                .toList();

        assertThat(ordered).containsExactly(first, second, third);
    }

    private static EmergencyCall call(Priority priority, Instant receivedAt) {
        return new EmergencyCall("+48123456789", CallType.FIRE, priority, "Main St 1", null, receivedAt);
    }
}
