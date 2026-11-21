package pl.kubit.emergency_call_dispatcher.dispatch;

import pl.kubit.emergency_call_dispatcher.call.EmergencyCall;

import java.util.Comparator;

/**
 * Decides the order in which queued calls are handed out to dispatchers (Strategy pattern).
 * <p>
 * The first call according to {@link #queueOrder()} is assigned next. The active implementation
 * is chosen with the {@code dispatch.strategy} property, see {@link DispatchConfig}.
 */
public interface QueueOrderingStrategy {

    Comparator<EmergencyCall> queueOrder();
}
