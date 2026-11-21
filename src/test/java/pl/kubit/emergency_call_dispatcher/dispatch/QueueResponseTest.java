package pl.kubit.emergency_call_dispatcher.dispatch;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pl.kubit.emergency_call_dispatcher.call.CallResponse;
import pl.kubit.emergency_call_dispatcher.call.CallType;
import pl.kubit.emergency_call_dispatcher.call.EmergencyCall;
import pl.kubit.emergency_call_dispatcher.call.Priority;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QueueResponseTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void shouldGroupByPriorityFromMostUrgent_whenCallsHaveDifferentPriorities() {
        // Order as the FIFO strategy would return it: priorities mixed.
        List<EmergencyCall> queue = List.of(
                call(1L, Priority.LOW),
                call(2L, Priority.CRITICAL),
                call(3L, Priority.HIGH));

        QueueResponse response = QueueResponse.from(queue);

        assertThat(response.size()).isEqualTo(3);
        assertThat(response.byPriority().keySet())
                .containsExactly(Priority.CRITICAL, Priority.HIGH, Priority.LOW);
    }

    @Test
    void shouldKeepStrategyOrderInsideGroup_whenCallsHaveSamePriority() {
        List<EmergencyCall> queue = List.of(
                call(5L, Priority.HIGH),
                call(2L, Priority.HIGH),
                call(9L, Priority.HIGH));

        QueueResponse response = QueueResponse.from(queue);

        assertThat(response.byPriority().get(Priority.HIGH))
                .extracting(CallResponse::id)
                .containsExactly(5L, 2L, 9L);
    }

    @Test
    void shouldReturnEmptyGroups_whenQueueIsEmpty() {
        QueueResponse response = QueueResponse.from(List.of());

        assertThat(response.size()).isZero();
        assertThat(response.byPriority()).isEmpty();
    }

    private static EmergencyCall call(Long id, Priority priority) {
        EmergencyCall call = new EmergencyCall("+48123456789", CallType.MEDICAL, priority, "Main St 1", null, T0);
        ReflectionTestUtils.setField(call, "id", id);
        return call;
    }
}
