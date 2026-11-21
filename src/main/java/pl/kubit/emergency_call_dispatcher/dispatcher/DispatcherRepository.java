package pl.kubit.emergency_call_dispatcher.dispatcher;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface DispatcherRepository extends JpaRepository<Dispatcher, Long> {

    /**
     * Dispatcher in the given status who has been waiting the longest.
     * Used with AVAILABLE to pick who gets the next call.
     * <p>
     * {@code PESSIMISTIC_WRITE} adds {@code FOR UPDATE} to the SQL: the row stays locked until the transaction ends.
     * When two calls arrive at the same moment, the second transaction waits instead of picking the same dispatcher;
     * after the first one commits, PostgreSQL sees that dispatcher is BUSY and returns the next AVAILABLE one (or none).
     * Without the lock, {@code @Version} would still prevent a double assignment, but the losing transaction would
     * be rolled back together with the new call it was saving.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Dispatcher> findFirstByStatusOrderByAvailableSinceAsc(DispatcherStatus status);
}
