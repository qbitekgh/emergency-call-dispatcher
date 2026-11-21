package pl.kubit.emergency_call_dispatcher.dispatcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.kubit.emergency_call_dispatcher.common.IllegalStatusTransitionException;
import pl.kubit.emergency_call_dispatcher.common.ResourceNotFoundException;
import pl.kubit.emergency_call_dispatcher.dispatch.DispatchService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatcherServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Mock
    private DispatcherRepository dispatcherRepository;

    @Mock
    private DispatchService dispatchService;

    private DispatcherService dispatcherService;

    @BeforeEach
    void setUp() {
        dispatcherService = new DispatcherService(dispatcherRepository, dispatchService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldSaveOfflineDispatcher_whenRegistered() {
        when(dispatcherRepository.save(any(Dispatcher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Dispatcher dispatcher = dispatcherService.register("Anna");

        assertThat(dispatcher.getName()).isEqualTo("Anna");
        assertThat(dispatcher.getStatus()).isEqualTo(DispatcherStatus.OFFLINE);
        verify(dispatcherRepository).save(dispatcher);
    }

    @Test
    void shouldReturnAllDispatchers_whenListed() {
        List<Dispatcher> dispatchers = List.of(new Dispatcher("Anna"));
        when(dispatcherRepository.findAll()).thenReturn(dispatchers);

        assertThat(dispatcherService.findAll()).isSameAs(dispatchers);
    }

    @Test
    void shouldThrowNotFound_whenDispatcherDoesNotExist() {
        when(dispatcherRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dispatcherService.changeStatus(99L, DispatcherStatus.AVAILABLE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void shouldMakeAvailableAndDispatchNext_whenOfflineDispatcherGoesAvailable() {
        Dispatcher dispatcher = new Dispatcher("Anna");
        when(dispatcherRepository.findById(1L)).thenReturn(Optional.of(dispatcher));

        Dispatcher result = dispatcherService.changeStatus(1L, DispatcherStatus.AVAILABLE);

        assertThat(result.getStatus()).isEqualTo(DispatcherStatus.AVAILABLE);
        assertThat(result.getAvailableSince()).isEqualTo(NOW);
        verify(dispatchService).dispatchNext();
    }

    @Test
    void shouldGoOfflineWithoutDispatching_whenAvailableDispatcherGoesOffline() {
        Dispatcher dispatcher = new Dispatcher("Anna");
        dispatcher.goAvailable(NOW.minusSeconds(60));
        when(dispatcherRepository.findById(1L)).thenReturn(Optional.of(dispatcher));

        Dispatcher result = dispatcherService.changeStatus(1L, DispatcherStatus.OFFLINE);

        assertThat(result.getStatus()).isEqualTo(DispatcherStatus.OFFLINE);
        assertThat(result.getAvailableSince()).isNull();
        verify(dispatchService, never()).dispatchNext();
    }

    @Test
    void shouldRejectChange_whenBusyIsRequestedManually() {
        Dispatcher dispatcher = new Dispatcher("Anna");
        dispatcher.goAvailable(NOW);
        when(dispatcherRepository.findById(1L)).thenReturn(Optional.of(dispatcher));

        assertThatThrownBy(() -> dispatcherService.changeStatus(1L, DispatcherStatus.BUSY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(dispatcher.getStatus()).isEqualTo(DispatcherStatus.AVAILABLE);
    }

    @Test
    void shouldThrowAndNotDispatch_whenOfflineDispatcherGoesOffline() {
        when(dispatcherRepository.findById(1L)).thenReturn(Optional.of(new Dispatcher("Anna")));

        assertThatThrownBy(() -> dispatcherService.changeStatus(1L, DispatcherStatus.OFFLINE))
                .isInstanceOf(IllegalStatusTransitionException.class);
        verify(dispatchService, never()).dispatchNext();
    }
}
