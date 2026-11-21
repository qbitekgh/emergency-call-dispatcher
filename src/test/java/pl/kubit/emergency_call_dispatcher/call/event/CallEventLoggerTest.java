package pl.kubit.emergency_call_dispatcher.call.event;

import org.junit.jupiter.api.Test;
import pl.kubit.emergency_call_dispatcher.call.CallType;
import pl.kubit.emergency_call_dispatcher.call.Priority;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class CallEventLoggerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final CallEventLogger logger = new CallEventLogger();

    @Test
    void shouldDescribeTypeAndPriority_whenCallIsReceived() {
        String message = logger.describe(new CallReceived(1L, CallType.FIRE, Priority.CRITICAL, NOW));

        assertThat(message).isEqualTo("Call 1 received (type=FIRE, priority=CRITICAL)");
    }

    @Test
    void shouldDescribeDispatcher_whenCallIsAssigned() {
        String message = logger.describe(new CallAssigned(1L, 3L, NOW));

        assertThat(message).isEqualTo("Call 1 assigned to dispatcher 3");
    }

    @Test
    void shouldDescribeDispatcher_whenCallIsClosed() {
        String message = logger.describe(new CallClosed(1L, 3L, NOW));

        assertThat(message).isEqualTo("Call 1 closed by dispatcher 3");
    }

    @Test
    void shouldDescribeCall_whenCallIsAbandoned() {
        String message = logger.describe(new CallAbandoned(1L, NOW));

        assertThat(message).isEqualTo("Call 1 abandoned by the caller");
    }

    @Test
    void shouldLogWithoutError_whenEventIsHandled() {
        assertThatCode(() -> logger.onCallEvent(new CallAbandoned(1L, NOW))).doesNotThrowAnyException();
    }
}
