package pl.kubit.emergency_call_dispatcher.call.event;

import java.time.Instant;

/**
 * The caller hung up while the call was still waiting in the queue.
 */
public record CallAbandoned(Long callId, Instant abandonedAt) implements CallEvent {
}
