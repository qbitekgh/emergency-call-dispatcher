package pl.kubit.emergency_call_dispatcher.dispatcher;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class DispatcherStatusTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "OFFLINE,   AVAILABLE",
            "AVAILABLE, BUSY",
            "AVAILABLE, OFFLINE",
            "BUSY,      AVAILABLE"
    })
    void shouldAllowTransition_whenTransitionIsDefined(DispatcherStatus from, DispatcherStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "OFFLINE,   OFFLINE",
            "OFFLINE,   BUSY",
            "AVAILABLE, AVAILABLE",
            "BUSY,      BUSY",
            "BUSY,      OFFLINE"
    })
    void shouldRejectTransition_whenTransitionIsNotDefined(DispatcherStatus from, DispatcherStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }
}
