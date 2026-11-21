package pl.kubit.emergency_call_dispatcher.stats;

import pl.kubit.emergency_call_dispatcher.call.CallStatus;
import pl.kubit.emergency_call_dispatcher.call.Priority;

import java.util.Map;

/**
 * Body of {@code GET /api/stats}.
 *
 * @param totalCalls         number of calls ever received
 * @param byStatus           number of calls in each status (every status is present)
 * @param byPriority         number of calls of each priority (every priority is present)
 * @param averageWaitSeconds average time from receiving to assigning a call; {@code null} when none was assigned
 */
public record CallStats(
        long totalCalls,
        Map<CallStatus, Long> byStatus,
        Map<Priority, Long> byPriority,
        Double averageWaitSeconds) {
}
