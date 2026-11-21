package pl.kubit.emergency_call_dispatcher.call.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Observer that writes every {@link CallEvent} to the log.
 * <p>
 * {@link TransactionalEventListener} runs after the publishing transaction has committed (its default phase),
 * so the log never reports a change that was later rolled back, for example after an optimistic locking conflict.
 */
@Slf4j
@Component
public class CallEventLogger {

    @TransactionalEventListener
    public void onCallEvent(CallEvent event) {
        log.info(describe(event));
    }

    /**
     * Pattern matching {@code switch} over a sealed interface: no {@code default} branch is needed,
     * and adding a new event type without a case here is a compile error.
     */
    String describe(CallEvent event) {
        return switch (event) {
            case CallReceived received -> "Call %d received (type=%s, priority=%s)"
                    .formatted(received.callId(), received.type(), received.priority());
            case CallAssigned assigned -> "Call %d assigned to dispatcher %d"
                    .formatted(assigned.callId(), assigned.dispatcherId());
            case CallClosed closed -> "Call %d closed by dispatcher %d"
                    .formatted(closed.callId(), closed.dispatcherId());
            case CallAbandoned abandoned -> "Call %d abandoned by the caller"
                    .formatted(abandoned.callId());
        };
    }
}
