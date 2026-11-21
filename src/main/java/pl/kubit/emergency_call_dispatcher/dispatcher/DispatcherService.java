package pl.kubit.emergency_call_dispatcher.dispatcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.kubit.emergency_call_dispatcher.common.ResourceNotFoundException;
import pl.kubit.emergency_call_dispatcher.dispatch.DispatchService;

import java.time.Clock;
import java.util.List;

/**
 * Use cases of a dispatcher. Status rules live in {@link Dispatcher};
 * this service loads and saves dispatchers and triggers dispatching when one becomes available.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DispatcherService {

    private final DispatcherRepository dispatcherRepository;
    private final DispatchService dispatchService;
    private final Clock clock;

    /**
     * Adds a new dispatcher; they start OFFLINE.
     */
    @Transactional
    public Dispatcher register(String name) {
        Dispatcher dispatcher = dispatcherRepository.save(new Dispatcher(name));
        log.info("Dispatcher {} registered", dispatcher.getId());
        return dispatcher;
    }

    @Transactional(readOnly = true)
    public List<Dispatcher> findAll() {
        return dispatcherRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Dispatcher findById(Long id) {
        return dispatcherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatcher", id));
    }

    /**
     * Manual status change by the dispatcher. Going AVAILABLE immediately pulls the next queued call.
     *
     * @throws IllegalArgumentException for BUSY, which is set only by assigning a call
     */
    @Transactional
    public Dispatcher changeStatus(Long id, DispatcherStatus target) {
        Dispatcher dispatcher = findById(id);
        switch (target) {
            case AVAILABLE -> {
                dispatcher.goAvailable(clock.instant());
                log.info("Dispatcher {} changed status to {}", id, target);
                dispatchService.dispatchNext();
            }
            case OFFLINE -> {
                dispatcher.goOffline();
                log.info("Dispatcher {} changed status to {}", id, target);
            }
            case BUSY -> throw new IllegalArgumentException("BUSY is set automatically when a call is assigned");
        }
        return dispatcher;
    }
}
