package pl.kubit.emergency_call_dispatcher.call.event;

import pl.kubit.emergency_call_dispatcher.call.CallType;
import pl.kubit.emergency_call_dispatcher.call.Priority;

import java.time.Instant;

/**
 * A new call was registered and put in the queue.
 * Carries only ids and enums: no caller number or location (personal data).
 */
public record CallReceived(Long callId, CallType type, Priority priority, Instant receivedAt) implements CallEvent {
}
