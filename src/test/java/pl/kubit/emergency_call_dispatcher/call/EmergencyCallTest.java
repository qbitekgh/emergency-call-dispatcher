package pl.kubit.emergency_call_dispatcher.call;

import org.junit.jupiter.api.Test;
import pl.kubit.emergency_call_dispatcher.common.IllegalStatusTransitionException;
import pl.kubit.emergency_call_dispatcher.dispatcher.Dispatcher;
import pl.kubit.emergency_call_dispatcher.dispatcher.DispatcherStatus;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmergencyCallTest {

    private static final Instant RECEIVED = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant ASSIGNED = RECEIVED.plusSeconds(30);
    private static final Instant CLOSED = RECEIVED.plusSeconds(600);

    @Test
    void shouldBeQueued_whenCreated() {
        EmergencyCall call = newCall();

        assertThat(call.getStatus()).isEqualTo(CallStatus.QUEUED);
        assertThat(call.getReceivedAt()).isEqualTo(RECEIVED);
        assertThat(call.getAssignedDispatcher()).isNull();
    }

    @Test
    void shouldAssignAndMakeDispatcherBusy_whenAssignedToAvailableDispatcher() {
        EmergencyCall call = newCall();
        Dispatcher dispatcher = availableDispatcher();

        call.assignTo(dispatcher, ASSIGNED);

        assertThat(call.getStatus()).isEqualTo(CallStatus.ASSIGNED);
        assertThat(call.getAssignedDispatcher()).isSameAs(dispatcher);
        assertThat(call.getAssignedAt()).isEqualTo(ASSIGNED);
        assertThat(dispatcher.getStatus()).isEqualTo(DispatcherStatus.BUSY);
    }

    @Test
    void shouldThrowAndLeaveCallQueued_whenDispatcherIsOffline() {
        EmergencyCall call = newCall();
        Dispatcher offline = new Dispatcher("Anna");

        assertThatThrownBy(() -> call.assignTo(offline, ASSIGNED))
                .isInstanceOf(IllegalStatusTransitionException.class);
        assertThat(call.getStatus()).isEqualTo(CallStatus.QUEUED);
        assertThat(call.getAssignedDispatcher()).isNull();
        assertThat(call.getAssignedAt()).isNull();
    }

    @Test
    void shouldThrowAndLeaveCallQueued_whenDispatcherIsBusy() {
        Dispatcher busy = availableDispatcher();
        newCall().assignTo(busy, ASSIGNED);
        EmergencyCall secondCall = newCall();

        assertThatThrownBy(() -> secondCall.assignTo(busy, ASSIGNED))
                .isInstanceOf(IllegalStatusTransitionException.class);
        assertThat(secondCall.getStatus()).isEqualTo(CallStatus.QUEUED);
        assertThat(secondCall.getAssignedDispatcher()).isNull();
    }

    @Test
    void shouldThrowAndKeepDispatcherAvailable_whenCallIsAlreadyAssigned() {
        EmergencyCall call = newCall();
        call.assignTo(availableDispatcher(), ASSIGNED);
        Dispatcher another = availableDispatcher();

        assertThatThrownBy(() -> call.assignTo(another, ASSIGNED))
                .isInstanceOf(IllegalStatusTransitionException.class);
        assertThat(another.getStatus()).isEqualTo(DispatcherStatus.AVAILABLE);
    }

    @Test
    void shouldCloseAndReleaseDispatcher_whenCallIsAssigned() {
        EmergencyCall call = newCall();
        Dispatcher dispatcher = availableDispatcher();
        call.assignTo(dispatcher, ASSIGNED);

        call.close(CLOSED);

        assertThat(call.getStatus()).isEqualTo(CallStatus.CLOSED);
        assertThat(call.getClosedAt()).isEqualTo(CLOSED);
        assertThat(dispatcher.getStatus()).isEqualTo(DispatcherStatus.AVAILABLE);
        assertThat(dispatcher.getAvailableSince()).isEqualTo(CLOSED);
    }

    @Test
    void shouldThrow_whenClosingQueuedCall() {
        EmergencyCall call = newCall();

        assertThatThrownBy(() -> call.close(CLOSED))
                .isInstanceOf(IllegalStatusTransitionException.class)
                .hasMessageContaining("QUEUED -> CLOSED");
        assertThat(call.getStatus()).isEqualTo(CallStatus.QUEUED);
    }

    @Test
    void shouldThrow_whenClosingAlreadyClosedCall() {
        EmergencyCall call = newCall();
        call.assignTo(availableDispatcher(), ASSIGNED);
        call.close(CLOSED);

        assertThatThrownBy(() -> call.close(CLOSED.plusSeconds(1)))
                .isInstanceOf(IllegalStatusTransitionException.class);
        assertThat(call.getClosedAt()).isEqualTo(CLOSED);
    }

    @Test
    void shouldAbandon_whenCallIsQueued() {
        EmergencyCall call = newCall();

        call.abandon(CLOSED);

        assertThat(call.getStatus()).isEqualTo(CallStatus.ABANDONED);
        assertThat(call.getClosedAt()).isEqualTo(CLOSED);
    }

    @Test
    void shouldThrowAndKeepDispatcherBusy_whenAbandoningAssignedCall() {
        EmergencyCall call = newCall();
        Dispatcher dispatcher = availableDispatcher();
        call.assignTo(dispatcher, ASSIGNED);

        assertThatThrownBy(() -> call.abandon(CLOSED))
                .isInstanceOf(IllegalStatusTransitionException.class);
        assertThat(call.getStatus()).isEqualTo(CallStatus.ASSIGNED);
        assertThat(dispatcher.getStatus()).isEqualTo(DispatcherStatus.BUSY);
    }

    private static EmergencyCall newCall() {
        return new EmergencyCall("+48123456789", CallType.MEDICAL, Priority.HIGH,
                "Warszawa, ul. Marszałkowska 1", "Unconscious person", RECEIVED);
    }

    private static Dispatcher availableDispatcher() {
        Dispatcher dispatcher = new Dispatcher("Anna");
        dispatcher.goAvailable(RECEIVED);
        return dispatcher;
    }
}
