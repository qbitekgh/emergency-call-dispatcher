package pl.kubit.emergency_call_dispatcher.call;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.kubit.emergency_call_dispatcher.common.ResourceNotFoundException;
import pl.kubit.emergency_call_dispatcher.dispatch.DispatchService;

import java.time.Clock;
import java.util.List;

/**
 * Use cases of an emergency call. Status rules live in {@link EmergencyCall};
 * this service loads and saves calls and triggers dispatching when needed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CallService {

    private final EmergencyCallRepository callRepository;
    private final DispatchService dispatchService;
    private final Clock clock;

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
        // Only id and enums are logged: caller number and location are personal data.
        log.info("Call {} received (type={}, priority={})", call.getId(), call.getType(), call.getPriority());
        dispatchService.dispatchNext();
        return call;
    }

    @Transactional(readOnly = true)
    public EmergencyCall findById(Long id) {
        return callRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Call", id));
    }

    /**
     * @param status filter; {@code null} returns calls in every status
     */
    @Transactional(readOnly = true)
    public List<EmergencyCall> findAll(CallStatus status) {
        return status == null ? callRepository.findAll() : callRepository.findByStatus(status);
    }

    /**
     * Closes the call; its dispatcher becomes available and gets the next queued call, if any.
     */
    @Transactional
    public EmergencyCall close(Long id) {
        EmergencyCall call = findById(id);
        call.close(clock.instant());
        log.info("Call {} closed by dispatcher {}", call.getId(), call.getAssignedDispatcher().getId());
        dispatchService.dispatchNext();
        return call;
    }

    @Transactional
    public EmergencyCall abandon(Long id) {
        EmergencyCall call = findById(id);
        call.abandon(clock.instant());
        log.info("Call {} abandoned", call.getId());
        return call;
    }
}
