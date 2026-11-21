package pl.kubit.emergency_call_dispatcher.stats;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.kubit.emergency_call_dispatcher.call.CallStatus;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final StatsRepository statsRepository;

    /**
     * One read-only transaction for all queries, so they run on the same database connection.
     */
    @Transactional(readOnly = true)
    public CallStats getStats() {
        Map<CallStatus, Long> byStatus = statsRepository.countByStatus();
        long total = byStatus.values().stream()
                .mapToLong(Long::longValue)
                .sum();
        return new CallStats(total, byStatus, statsRepository.countByPriority(), statsRepository.averageWaitSeconds());
    }
}
