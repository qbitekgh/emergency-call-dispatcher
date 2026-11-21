package pl.kubit.emergency_call_dispatcher.dispatch;

import org.junit.jupiter.api.Test;
import pl.kubit.emergency_call_dispatcher.call.CallType;
import pl.kubit.emergency_call_dispatcher.call.EmergencyCall;
import pl.kubit.emergency_call_dispatcher.call.Priority;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PriorityThenFifoStrategyTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    private final QueueOrderingStrategy strategy = new PriorityThenFifoStrategy();

    @Test
    void shouldOrderByPriority_whenPrioritiesDiffer() {
        EmergencyCall low = call(Priority.LOW, T0);
        EmergencyCall critical = call(Priority.CRITICAL, T0.plusSeconds(30));
        EmergencyCall medium = call(Priority.MEDIUM, T0.plusSeconds(10));
        EmergencyCall high = call(Priority.HIGH, T0.plusSeconds(20));

        List<EmergencyCall> ordered = sort(low, critical, medium, high);

        assertThat(ordered).containsExactly(critical, high, medium, low);
    }

    @Test
    void shouldPutEarlierCallFirst_whenPrioritiesAreEqual() {
        EmergencyCall later = call(Priority.HIGH, T0.plusSeconds(60));
        EmergencyCall earlier = call(Priority.HIGH, T0);

        List<EmergencyCall> ordered = sort(later, earlier);

        assertThat(ordered).containsExactly(earlier, later);
    }

    @Test
    void shouldPutOlderLowerPriorityCallsAfterNewerUrgentOne_whenMixed() {
        EmergencyCall oldLow = call(Priority.LOW, T0);
        EmergencyCall oldCritical = call(Priority.CRITICAL, T0.plusSeconds(10));
        EmergencyCall newCritical = call(Priority.CRITICAL, T0.plusSeconds(20));

        List<EmergencyCall> ordered = sort(oldLow, newCritical, oldCritical);

        assertThat(ordered).containsExactly(oldCritical, newCritical, oldLow);
    }

    private List<EmergencyCall> sort(EmergencyCall... calls) {
        return List.of(calls).stream().sorted(strategy.queueOrder()).toList();
    }

    private static EmergencyCall call(Priority priority, Instant receivedAt) {
        return new EmergencyCall("+48123456789", CallType.MEDICAL, priority, "Main St 1", null, receivedAt);
    }
}
