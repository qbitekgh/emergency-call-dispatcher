package pl.kubit.emergency_call_dispatcher.dispatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.kubit.emergency_call_dispatcher.call.CallStatus;
import pl.kubit.emergency_call_dispatcher.call.CallType;
import pl.kubit.emergency_call_dispatcher.call.EmergencyCall;
import pl.kubit.emergency_call_dispatcher.call.EmergencyCallRepository;
import pl.kubit.emergency_call_dispatcher.call.Priority;
import pl.kubit.emergency_call_dispatcher.dispatcher.Dispatcher;
import pl.kubit.emergency_call_dispatcher.dispatcher.DispatcherRepository;
import pl.kubit.emergency_call_dispatcher.dispatcher.DispatcherStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchServiceTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant NOW = T0.plusSeconds(300);

    @Mock
    private EmergencyCallRepository callRepository;

    @Mock
    private DispatcherRepository dispatcherRepository;

    private DispatchService dispatchService;

    @BeforeEach
    void setUp() {
        // Real strategy instead of a mock: it is a pure object and its ordering is part of what we verify.
        dispatchService = new DispatchService(callRepository, dispatcherRepository,
                new PriorityThenFifoStrategy(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldAssignFirstCallByStrategyToLongestWaitingDispatcher_whenBothExist() {
        Dispatcher dispatcher = availableDispatcher();
        EmergencyCall oldLow = call(Priority.LOW, T0);
        EmergencyCall critical = call(Priority.CRITICAL, T0.plusSeconds(60));
        when(dispatcherRepository.findFirstByStatusOrderByAvailableSinceAsc(DispatcherStatus.AVAILABLE))
                .thenReturn(Optional.of(dispatcher));
        when(callRepository.findByStatus(CallStatus.QUEUED)).thenReturn(List.of(oldLow, critical));

        Optional<EmergencyCall> assigned = dispatchService.dispatchNext();

        assertThat(assigned).containsSame(critical);
        assertThat(critical.getStatus()).isEqualTo(CallStatus.ASSIGNED);
        assertThat(critical.getAssignedDispatcher()).isSameAs(dispatcher);
        assertThat(critical.getAssignedAt()).isEqualTo(NOW);
        assertThat(dispatcher.getStatus()).isEqualTo(DispatcherStatus.BUSY);
        assertThat(oldLow.getStatus()).isEqualTo(CallStatus.QUEUED);
    }

    @Test
    void shouldAssignNothing_whenNoDispatcherIsAvailable() {
        when(dispatcherRepository.findFirstByStatusOrderByAvailableSinceAsc(DispatcherStatus.AVAILABLE))
                .thenReturn(Optional.empty());

        Optional<EmergencyCall> assigned = dispatchService.dispatchNext();

        assertThat(assigned).isEmpty();
        verifyNoInteractions(callRepository);
    }

    @Test
    void shouldAssignNothingAndKeepDispatcherAvailable_whenQueueIsEmpty() {
        Dispatcher dispatcher = availableDispatcher();
        when(dispatcherRepository.findFirstByStatusOrderByAvailableSinceAsc(DispatcherStatus.AVAILABLE))
                .thenReturn(Optional.of(dispatcher));
        when(callRepository.findByStatus(CallStatus.QUEUED)).thenReturn(List.of());

        Optional<EmergencyCall> assigned = dispatchService.dispatchNext();

        assertThat(assigned).isEmpty();
        assertThat(dispatcher.getStatus()).isEqualTo(DispatcherStatus.AVAILABLE);
    }

    @Test
    void shouldReturnQueuedCallsInStrategyOrder_whenQueueIsRequested() {
        EmergencyCall low = call(Priority.LOW, T0);
        EmergencyCall high = call(Priority.HIGH, T0.plusSeconds(30));
        EmergencyCall critical = call(Priority.CRITICAL, T0.plusSeconds(60));
        when(callRepository.findByStatus(CallStatus.QUEUED)).thenReturn(List.of(low, high, critical));

        List<EmergencyCall> queue = dispatchService.queue();

        assertThat(queue).containsExactly(critical, high, low);
    }

    private static Dispatcher availableDispatcher() {
        Dispatcher dispatcher = new Dispatcher("Anna");
        dispatcher.goAvailable(T0);
        return dispatcher;
    }

    private static EmergencyCall call(Priority priority, Instant receivedAt) {
        return new EmergencyCall("+48123456789", CallType.POLICE, priority, "Main St 1", null, receivedAt);
    }
}
