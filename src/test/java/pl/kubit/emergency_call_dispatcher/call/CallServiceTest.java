package pl.kubit.emergency_call_dispatcher.call;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import pl.kubit.emergency_call_dispatcher.common.IllegalStatusTransitionException;
import pl.kubit.emergency_call_dispatcher.common.ResourceNotFoundException;
import pl.kubit.emergency_call_dispatcher.dispatch.DispatchService;
import pl.kubit.emergency_call_dispatcher.dispatcher.Dispatcher;
import pl.kubit.emergency_call_dispatcher.dispatcher.DispatcherStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallServiceTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant NOW = T0.plusSeconds(600);

    @Mock
    private EmergencyCallRepository callRepository;

    @Mock
    private DispatchService dispatchService;

    private CallService callService;

    @BeforeEach
    void setUp() {
        callService = new CallService(callRepository, dispatchService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldSaveQueuedCallAndTryToDispatchIt_whenCallIsReceived() {
        when(callRepository.save(any(EmergencyCall.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReceiveCallCommand command = new ReceiveCallCommand(
                "+48123456789", CallType.FIRE, Priority.CRITICAL, "Main St 1", "Kitchen on fire");

        EmergencyCall call = callService.receive(command);

        assertThat(call.getStatus()).isEqualTo(CallStatus.QUEUED);
        assertThat(call.getReceivedAt()).isEqualTo(NOW);
        assertThat(call.getCallerNumber()).isEqualTo("+48123456789");
        assertThat(call.getType()).isEqualTo(CallType.FIRE);
        assertThat(call.getPriority()).isEqualTo(Priority.CRITICAL);
        assertThat(call.getLocation()).isEqualTo("Main St 1");
        assertThat(call.getDescription()).isEqualTo("Kitchen on fire");
        var inOrder = inOrder(callRepository, dispatchService);
        inOrder.verify(callRepository).save(call);
        inOrder.verify(dispatchService).dispatchNext();
    }

    @Test
    void shouldReturnCall_whenCallExists() {
        EmergencyCall call = queuedCall();
        when(callRepository.findById(1L)).thenReturn(Optional.of(call));

        assertThat(callService.findById(1L)).isSameAs(call);
    }

    @Test
    void shouldThrowNotFound_whenCallDoesNotExist() {
        when(callRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> callService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void shouldReturnPageOfAllCalls_whenStatusFilterIsNull() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<EmergencyCall> page = new PageImpl<>(List.of(queuedCall()));
        when(callRepository.findAll(pageable)).thenReturn(page);

        assertThat(callService.findAll(null, pageable)).isSameAs(page);
    }

    @Test
    void shouldReturnPageOfCallsWithStatus_whenStatusFilterIsGiven() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<EmergencyCall> page = new PageImpl<>(List.of(queuedCall()));
        when(callRepository.findByStatus(CallStatus.QUEUED, pageable)).thenReturn(page);

        assertThat(callService.findAll(CallStatus.QUEUED, pageable)).isSameAs(page);
    }

    @Test
    void shouldCloseCallFreeDispatcherAndDispatchNext_whenAssignedCallIsClosed() {
        Dispatcher dispatcher = new Dispatcher("Anna");
        dispatcher.goAvailable(T0);
        EmergencyCall call = queuedCall();
        call.assignTo(dispatcher, T0.plusSeconds(10));
        when(callRepository.findById(1L)).thenReturn(Optional.of(call));

        EmergencyCall closed = callService.close(1L);

        assertThat(closed.getStatus()).isEqualTo(CallStatus.CLOSED);
        assertThat(closed.getClosedAt()).isEqualTo(NOW);
        assertThat(dispatcher.getStatus()).isEqualTo(DispatcherStatus.AVAILABLE);
        assertThat(dispatcher.getAvailableSince()).isEqualTo(NOW);
        verify(dispatchService).dispatchNext();
    }

    @Test
    void shouldThrowAndNotDispatch_whenClosingQueuedCall() {
        when(callRepository.findById(1L)).thenReturn(Optional.of(queuedCall()));

        assertThatThrownBy(() -> callService.close(1L))
                .isInstanceOf(IllegalStatusTransitionException.class);
        verify(dispatchService, never()).dispatchNext();
    }

    @Test
    void shouldAbandonCallWithoutDispatching_whenQueuedCallIsAbandoned() {
        EmergencyCall call = queuedCall();
        when(callRepository.findById(1L)).thenReturn(Optional.of(call));

        EmergencyCall abandoned = callService.abandon(1L);

        assertThat(abandoned.getStatus()).isEqualTo(CallStatus.ABANDONED);
        assertThat(abandoned.getClosedAt()).isEqualTo(NOW);
        verify(dispatchService, never()).dispatchNext();
    }

    private static EmergencyCall queuedCall() {
        return new EmergencyCall("+48123456789", CallType.MEDICAL, Priority.HIGH, "Main St 1", null, T0);
    }
}
