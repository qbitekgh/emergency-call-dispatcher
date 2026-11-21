package pl.kubit.emergency_call_dispatcher.call;

/**
 * Lifecycle of an emergency call.
 * <pre>
 * QUEUED ──► ASSIGNED ──► CLOSED
 *    │
 *    └─────► ABANDONED
 * </pre>
 * CLOSED and ABANDONED are terminal states.
 */
public enum CallStatus {
    QUEUED,
    ASSIGNED,
    CLOSED,
    ABANDONED;

    public boolean canTransitionTo(CallStatus target) {
        return switch (this) {
            case QUEUED -> target == ASSIGNED || target == ABANDONED;
            case ASSIGNED -> target == CLOSED;
            case CLOSED, ABANDONED -> false;
        };
    }
}
