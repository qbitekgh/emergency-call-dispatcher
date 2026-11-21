package pl.kubit.emergency_call_dispatcher.dispatcher;

/**
 * Availability of a dispatcher.
 * <pre>
 * OFFLINE ──► AVAILABLE ◄──► BUSY
 *                 │
 *                 └──► OFFLINE
 * </pre>
 * A BUSY dispatcher cannot go OFFLINE directly: that would leave their assigned call without anyone handling it.
 */
public enum DispatcherStatus {
    AVAILABLE,
    BUSY,
    OFFLINE;

    public boolean canTransitionTo(DispatcherStatus target) {
        return switch (this) {
            case OFFLINE -> target == AVAILABLE;
            case AVAILABLE -> target == BUSY || target == OFFLINE;
            case BUSY -> target == AVAILABLE;
        };
    }
}
