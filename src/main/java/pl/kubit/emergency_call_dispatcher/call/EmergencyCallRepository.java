package pl.kubit.emergency_call_dispatcher.call;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmergencyCallRepository extends JpaRepository<EmergencyCall, Long> {

    /** All calls in the given status; used to build the queue. */
    List<EmergencyCall> findByStatus(CallStatus status);

    /** One page of calls in the given status; used by the list endpoint. */
    Page<EmergencyCall> findByStatus(CallStatus status, Pageable pageable);
}
