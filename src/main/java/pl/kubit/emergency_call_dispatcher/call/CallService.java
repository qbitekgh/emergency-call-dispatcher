package pl.kubit.emergency_call_dispatcher.call;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.kubit.emergency_call_dispatcher.call.event.CallAbandoned;
import pl.kubit.emergency_call_dispatcher.call.event.CallClosed;
import pl.kubit.emergency_call_dispatcher.call.event.CallReceived;
import pl.kubit.emergency_call_dispatcher.common.ResourceNotFoundException;
import pl.kubit.emergency_call_dispatcher.dispatch.DispatchService;

import java.time.Clock;

/**
 * Use cases of an emergency call. Status rules live in {@link EmergencyCall};
 * this service loads and saves calls, triggers dispatching when needed and publishes
 * {@link pl.kubit.emergency_call_dispatcher.call.event.CallEvent}s about what happened.
 */
@Service
@RequiredArgsConstructor
public class CallService {

    private final EmergencyCallRepository callRepository;
    private final DispatchService dispatchService;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Registers a new call and immediately tries to assign it to an available dispatcher.
     */
    @Transactional
    public EmergencyCall receive(ReceiveCallCommand command) {
        EmergencyCall call = callRepository.save(new EmergencyCall(
                command.callerNumber(),
                command.type(),
                command.priority(),
                command.location(),
                command.description(),
                clock.instant()));
        eventPublisher.publishEvent(
                new CallReceived(call.getId(), call.getType(), call.getPriority(), call.getReceivedAt()));
        dispatchService.dispatchNext();
        return call;
    }

    @Transactional(readOnly = true)
    public EmergencyCall findById(Long id) {
        return callRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Call", id));
    }

    /**
     * @param status   filter; {@code null} returns calls in every status
     * @param pageable page number, page size and sort order
     */
    @Transactional(readOnly = true)
    public Page<EmergencyCall> findAll(CallStatus status, Pageable pageable) {
        return status == null
                ? callRepository.findAll(pageable)
                : callRepository.findByStatus(status, pageable);
    }

    /**
     * Closes the call; its dispatcher becomes available and gets the next queued call, if any.
     */
    @Transactional
    public EmergencyCall close(Long id) {
        EmergencyCall call = findById(id);
        call.close(clock.instant());
        eventPublisher.publishEvent(
                new CallClosed(call.getId(), call.getAssignedDispatcher().getId(), call.getClosedAt()));
        dispatchService.dispatchNext();
        return call;
    }

    @Transactional
    public EmergencyCall abandon(Long id) {
        EmergencyCall call = findById(id);
        call.abandon(clock.instant());
        eventPublisher.publishEvent(new CallAbandoned(call.getId(), call.getClosedAt()));
        return call;
    }
}
