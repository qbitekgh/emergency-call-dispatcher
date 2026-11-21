package pl.kubit.emergency_call_dispatcher.call;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmergencyCallRepository extends JpaRepository<EmergencyCall, Long> {

    List<EmergencyCall> findByStatus(CallStatus status);
}
