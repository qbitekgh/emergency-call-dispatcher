package pl.kubit.emergency_call_dispatcher.dispatch;

import pl.kubit.emergency_call_dispatcher.call.EmergencyCall;

import java.util.Comparator;

/**
 * Calls are handled strictly in the order they were received; priority is ignored.
 */
public class FifoStrategy implements QueueOrderingStrategy {

    private static final Comparator<EmergencyCall> ORDER = Comparator.comparing(EmergencyCall::getReceivedAt);

    @Override
    public Comparator<EmergencyCall> queueOrder() {
        return ORDER;
    }
}
