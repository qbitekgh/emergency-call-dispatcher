package pl.kubit.emergency_call_dispatcher.call.event;

/**
 * Something that happened to an emergency call (Observer pattern).
 * <p>
 * Services publish these events with Spring's {@code ApplicationEventPublisher} and do not know who listens.
 * The interface is {@code sealed}: the compiler knows every possible event, so a {@code switch} over
 * a {@code CallEvent} needs no {@code default} and fails to compile when a new event type is added
 * but not handled.
 */
public sealed interface CallEvent permits CallReceived, CallAssigned, CallClosed, CallAbandoned {

    Long callId();
}
