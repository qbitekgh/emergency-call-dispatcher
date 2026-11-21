package pl.kubit.emergency_call_dispatcher.dispatcher;

import java.time.Instant;

/**
 * A dispatcher as returned by the API.
 */
public record DispatcherResponse(
        Long id,
        String name,
        DispatcherStatus status,
        Instant availableSince) {

    public static DispatcherResponse from(Dispatcher dispatcher) {
        return new DispatcherResponse(
                dispatcher.getId(),
                dispatcher.getName(),
                dispatcher.getStatus(),
                dispatcher.getAvailableSince());
    }
}
