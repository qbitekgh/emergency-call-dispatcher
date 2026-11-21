package pl.kubit.emergency_call_dispatcher.dispatch;

import pl.kubit.emergency_call_dispatcher.call.EmergencyCall;

import java.util.Comparator;

/**
 * More urgent calls first; among calls of equal priority, the one received earlier goes first.
 * Relies on {@link pl.kubit.emergency_call_dispatcher.call.Priority} constants being declared
 * from the most to the least urgent.
 */
public class PriorityThenFifoStrategy implements QueueOrderingStrategy {

    private static final Comparator<EmergencyCall> ORDER =
            Comparator.comparing(EmergencyCall::getPriority)
                    .thenComparing(EmergencyCall::getReceivedAt);

    @Override
    public Comparator<EmergencyCall> queueOrder() {
        return ORDER;
    }
}
