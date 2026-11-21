package pl.kubit.emergency_call_dispatcher.dispatch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.kubit.emergency_call_dispatcher.call.CallStatus;
import pl.kubit.emergency_call_dispatcher.call.EmergencyCall;
import pl.kubit.emergency_call_dispatcher.call.EmergencyCallRepository;
import pl.kubit.emergency_call_dispatcher.call.event.CallAssigned;
import pl.kubit.emergency_call_dispatcher.dispatcher.Dispatcher;
import pl.kubit.emergency_call_dispatcher.dispatcher.DispatcherRepository;
import pl.kubit.emergency_call_dispatcher.dispatcher.DispatcherStatus;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

/**
 * Pairs queued calls with available dispatchers.
 * <p>
 * The system keeps one rule: there is never an AVAILABLE dispatcher while a call is QUEUED.
 * Only two events can break it: a new call arrives, or one dispatcher becomes available
 * (going AVAILABLE or closing a call). Each adds exactly one call or one dispatcher,
 * so a single pairing via {@link #dispatchNext()} is enough to restore the rule.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchService {

    private final EmergencyCallRepository callRepository;
    private final DispatcherRepository dispatcherRepository;
    private final QueueOrderingStrategy strategy;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Assigns the first queued call to the dispatcher who has been available the longest.
     * Joins the caller's transaction, so the triggering change and the assignment commit together.
     *
     * @return the assigned call, or empty when there is no available dispatcher or no queued call
     */
    @Transactional
    public Optional<EmergencyCall> dispatchNext() {
        Optional<EmergencyCall> assigned =
                dispatcherRepository.findFirstByStatusOrderByAvailableSinceAsc(DispatcherStatus.AVAILABLE)
                        .flatMap(dispatcher -> nextQueuedCall().map(call -> assign(call, dispatcher)));
        if (assigned.isEmpty()) {
            log.debug("Nothing to dispatch: no available dispatcher or no queued call");
        }
        return assigned;
    }

    /**
     * All queued calls in the order they will be handed out by the active strategy.
     */
    @Transactional(readOnly = true)
    public List<EmergencyCall> queue() {
        return callRepository.findByStatus(CallStatus.QUEUED).stream()
                .sorted(strategy.queueOrder())
                .toList();
    }

    private Optional<EmergencyCall> nextQueuedCall() {
        return callRepository.findByStatus(CallStatus.QUEUED).stream()
                .min(strategy.queueOrder());
    }

    private EmergencyCall assign(EmergencyCall call, Dispatcher dispatcher) {
        call.assignTo(dispatcher, clock.instant());
        eventPublisher.publishEvent(new CallAssigned(call.getId(), dispatcher.getId(), call.getAssignedAt()));
        return call;
    }
}
