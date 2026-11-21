package pl.kubit.emergency_call_dispatcher.call;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CallStatusTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "QUEUED,   ASSIGNED",
            "QUEUED,   ABANDONED",
            "ASSIGNED, CLOSED"
    })
    void shouldAllowTransition_whenTransitionIsDefined(CallStatus from, CallStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "QUEUED,    QUEUED",
            "QUEUED,    CLOSED",
            "ASSIGNED,  QUEUED",
            "ASSIGNED,  ASSIGNED",
            "ASSIGNED,  ABANDONED",
            "CLOSED,    QUEUED",
            "CLOSED,    ASSIGNED",
            "CLOSED,    CLOSED",
            "CLOSED,    ABANDONED",
            "ABANDONED, QUEUED",
            "ABANDONED, ASSIGNED",
            "ABANDONED, CLOSED",
            "ABANDONED, ABANDONED"
    })
    void shouldRejectTransition_whenTransitionIsNotDefined(CallStatus from, CallStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }
}
