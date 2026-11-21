package pl.kubit.emergency_call_dispatcher.common;

/**
 * Thrown when a domain object is asked to move to a status that is not reachable from its current one.
 * Shared by calls and dispatchers; mapped to HTTP 409 Conflict at the API boundary.
 */
public class IllegalStatusTransitionException extends RuntimeException {

    private final Enum<?> from;
    private final Enum<?> to;

    public IllegalStatusTransitionException(Enum<?> from, Enum<?> to) {
        super("Illegal status transition for %s: %s -> %s"
                .formatted(from.getDeclaringClass().getSimpleName(), from, to));
        this.from = from;
        this.to = to;
    }

    public Enum<?> getFrom() {
        return from;
    }

    public Enum<?> getTo() {
        return to;
    }
}
