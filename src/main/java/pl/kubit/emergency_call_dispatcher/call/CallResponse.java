package pl.kubit.emergency_call_dispatcher.call;

import pl.kubit.emergency_call_dispatcher.dispatcher.Dispatcher;

import java.time.Instant;

/**
 * An emergency call as returned by the API. Entities are never returned from controllers:
 * the API format stays stable when the entity changes, and no lazy JPA relation is serialized by accident.
 */
public record CallResponse(
        Long id,
        String callerNumber,
        CallType type,
        Priority priority,
        String location,
        String description,
        CallStatus status,
        Instant receivedAt,
        Instant assignedAt,
        Instant closedAt,
        Long assignedDispatcherId) {

    public static CallResponse from(EmergencyCall call) {
        Dispatcher dispatcher = call.getAssignedDispatcher();
        return new CallResponse(
                call.getId(),
                call.getCallerNumber(),
                call.getType(),
                call.getPriority(),
                call.getLocation(),
                call.getDescription(),
                call.getStatus(),
                call.getReceivedAt(),
                call.getAssignedAt(),
                call.getClosedAt(),
                // Only the id: Hibernate can read it from a lazy proxy without querying the database.
                dispatcher == null ? null : dispatcher.getId());
    }
}
