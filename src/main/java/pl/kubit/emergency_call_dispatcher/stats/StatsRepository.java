package pl.kubit.emergency_call_dispatcher.stats;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;
import pl.kubit.emergency_call_dispatcher.call.CallStatus;
import pl.kubit.emergency_call_dispatcher.call.Priority;

import java.util.EnumMap;
import java.util.Map;

/**
 * Aggregate queries over emergency calls, written in plain SQL with {@link JdbcTemplate}.
 * <p>
 * JPA is good at loading and saving single entities; for reports the database can count and average
 * much faster than loading every call into memory, so here the SQL is written by hand.
 */
@Repository
@RequiredArgsConstructor
public class StatsRepository {

    private static final String COUNT_BY_STATUS = """
            SELECT status AS name, COUNT(*) AS total
            FROM emergency_call
            GROUP BY status
            """;

    private static final String COUNT_BY_PRIORITY = """
            SELECT priority AS name, COUNT(*) AS total
            FROM emergency_call
            GROUP BY priority
            """;

    /** EXTRACT(EPOCH FROM interval) turns the time between receiving and assigning into seconds. */
    private static final String AVERAGE_WAIT_SECONDS = """
            SELECT AVG(EXTRACT(EPOCH FROM (assigned_at - received_at)))
            FROM emergency_call
            WHERE assigned_at IS NOT NULL
            """;

    private final JdbcTemplate jdbcTemplate;

    /**
     * @return number of calls in every status; statuses without calls are present with 0
     */
    public Map<CallStatus, Long> countByStatus() {
        return countBy(COUNT_BY_STATUS, CallStatus.class);
    }

    /**
     * @return number of calls of every priority; priorities without calls are present with 0
     */
    public Map<Priority, Long> countByPriority() {
        return countBy(COUNT_BY_PRIORITY, Priority.class);
    }

    /**
     * @return average seconds from receiving a call to assigning it, or {@code null} when no call was assigned yet
     */
    public Double averageWaitSeconds() {
        return jdbcTemplate.queryForObject(AVERAGE_WAIT_SECONDS, Double.class);
    }

    /**
     * Runs a {@code GROUP BY} query returning columns {@code name} and {@code total}, and puts the result
     * into an {@link EnumMap} that starts with 0 for every constant of {@code type}.
     */
    private <E extends Enum<E>> Map<E, Long> countBy(String sql, Class<E> type) {
        Map<E, Long> counts = new EnumMap<>(type);
        for (E constant : type.getEnumConstants()) {
            counts.put(constant, 0L);
        }
        RowCallbackHandler addRow = rs -> counts.put(Enum.valueOf(type, rs.getString("name")), rs.getLong("total"));
        jdbcTemplate.query(sql, addRow);
        return counts;
    }
}
