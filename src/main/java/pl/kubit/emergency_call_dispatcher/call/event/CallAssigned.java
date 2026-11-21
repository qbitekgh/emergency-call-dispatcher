package pl.kubit.emergency_call_dispatcher.call.event;

import java.time.Instant;

/**
 * A queued call was assigned to an available dispatcher.
 */
public record CallAssigned(Long callId, Long dispatcherId, Instant assignedAt) implements CallEvent {
}
