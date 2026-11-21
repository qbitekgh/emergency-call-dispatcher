package pl.kubit.emergency_call_dispatcher.dispatcher;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DispatcherRepository extends JpaRepository<Dispatcher, Long> {

    /**
     * Dispatcher in the given status who has been waiting the longest.
     * Used with AVAILABLE to pick who gets the next call.
     */
    Optional<Dispatcher> findFirstByStatusOrderByAvailableSinceAsc(DispatcherStatus status);
}
