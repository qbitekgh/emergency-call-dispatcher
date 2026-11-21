package pl.kubit.emergency_call_dispatcher.dispatch;

import pl.kubit.emergency_call_dispatcher.call.CallResponse;
import pl.kubit.emergency_call_dispatcher.call.EmergencyCall;
import pl.kubit.emergency_call_dispatcher.call.Priority;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Body of {@code GET /api/queue}: queued calls grouped by priority.
 *
 * @param size       number of queued calls
 * @param byPriority calls of each priority, from CRITICAL to LOW; priorities with no calls are left out.
 *                   Within a group the calls keep the order of the active {@link QueueOrderingStrategy}.
 */
public record QueueResponse(int size, Map<Priority, List<CallResponse>> byPriority) {

    /**
     * @param orderedQueue queued calls already sorted by the active strategy
     */
    public static QueueResponse from(List<EmergencyCall> orderedQueue) {
        Map<Priority, List<CallResponse>> byPriority = orderedQueue.stream()
                .collect(Collectors.groupingBy(
                        EmergencyCall::getPriority,
                        // EnumMap keeps keys in declaration order: CRITICAL, HIGH, MEDIUM, LOW
                        () -> new EnumMap<>(Priority.class),
                        // toList() collects in encounter order, so the strategy's order survives grouping
                        Collectors.mapping(CallResponse::from, Collectors.toList())));
        return new QueueResponse(orderedQueue.size(), byPriority);
    }
}
