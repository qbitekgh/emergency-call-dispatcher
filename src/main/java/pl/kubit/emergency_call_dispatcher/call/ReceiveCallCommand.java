package pl.kubit.emergency_call_dispatcher.call;

/**
 * Data needed to register a new emergency call. Input of {@link CallService#receive};
 * keeps the service independent of the web layer's request DTOs.
 */
public record ReceiveCallCommand(
        String callerNumber,
        CallType type,
        Priority priority,
        String location,
        String description) {
}
