package pl.kubit.emergency_call_dispatcher.call;

/**
 * Urgency of an emergency call.
 * <p>
 * Constants are declared from the most to the least urgent, so the natural enum order
 * ({@link Enum#compareTo}) can be used directly when sorting the queue.
 * Do not reorder them.
 */
public enum Priority {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}
