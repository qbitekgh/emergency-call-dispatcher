package pl.kubit.emergency_call_dispatcher.dispatcher;

import org.junit.jupiter.api.Test;
import pl.kubit.emergency_call_dispatcher.common.IllegalStatusTransitionException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DispatcherTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void shouldBeOffline_whenCreated() {
        Dispatcher dispatcher = new Dispatcher("Anna");

        assertThat(dispatcher.getStatus()).isEqualTo(DispatcherStatus.OFFLINE);
        assertThat(dispatcher.getAvailableSince()).isNull();
    }

    @Test
    void shouldBecomeAvailableAndRecordTime_whenGoingAvailableFromOffline() {
        Dispatcher dispatcher = new Dispatcher("Anna");

        dispatcher.goAvailable(NOW);

        assertThat(dispatcher.getStatus()).isEqualTo(DispatcherStatus.AVAILABLE);
        assertThat(dispatcher.getAvailableSince()).isEqualTo(NOW);
    }

    @Test
    void shouldBecomeBusyAndClearAvailableSince_whenMarkedBusyWhileAvailable() {
        Dispatcher dispatcher = new Dispatcher("Anna");
        dispatcher.goAvailable(NOW);

        dispatcher.markBusy();

        assertThat(dispatcher.getStatus()).isEqualTo(DispatcherStatus.BUSY);
        assertThat(dispatcher.getAvailableSince()).isNull();
    }

    @Test
    void shouldGoOfflineAndClearAvailableSince_whenGoingOfflineWhileAvailable() {
        Dispatcher dispatcher = new Dispatcher("Anna");
        dispatcher.goAvailable(NOW);

        dispatcher.goOffline();

        assertThat(dispatcher.getStatus()).isEqualTo(DispatcherStatus.OFFLINE);
        assertThat(dispatcher.getAvailableSince()).isNull();
    }

    @Test
    void shouldThrow_whenMarkedBusyWhileOffline() {
        Dispatcher dispatcher = new Dispatcher("Anna");

        assertThatThrownBy(dispatcher::markBusy)
                .isInstanceOf(IllegalStatusTransitionException.class);
        assertThat(dispatcher.getStatus()).isEqualTo(DispatcherStatus.OFFLINE);
    }

    @Test
    void shouldThrow_whenGoingOfflineWhileBusy() {
        Dispatcher dispatcher = new Dispatcher("Anna");
        dispatcher.goAvailable(NOW);
        dispatcher.markBusy();

        assertThatThrownBy(dispatcher::goOffline)
                .isInstanceOf(IllegalStatusTransitionException.class)
                .hasMessageContaining("BUSY -> OFFLINE");
        assertThat(dispatcher.getStatus()).isEqualTo(DispatcherStatus.BUSY);
    }
}
