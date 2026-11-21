package pl.kubit.emergency_call_dispatcher.call.event;

import java.time.Instant;

/**
 * The dispatcher finished handling the call.
 */
public record CallClosed(Long callId, Long dispatcherId, Instant closedAt) implements CallEvent {
}
