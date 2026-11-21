package pl.kubit.emergency_call_dispatcher.dispatcher;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import pl.kubit.emergency_call_dispatcher.common.IllegalStatusTransitionException;

import java.time.Instant;
import java.util.Objects;

/**
 * Person handling emergency calls.
 * <p>
 * State changes only through domain methods; each one validates the transition
 * with {@link DispatcherStatus#canTransitionTo}. {@code availableSince} is set only while the
 * dispatcher is AVAILABLE and is used to pick the one who has been waiting the longest.
 */
@Entity
@Table(name = "dispatcher")
public class Dispatcher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DispatcherStatus status;

    private Instant availableSince;

    @Version
    private Long version;

    protected Dispatcher() {
        // required by JPA
    }

    public Dispatcher(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.status = DispatcherStatus.OFFLINE;
    }

    public void goAvailable(Instant now) {
        transitionTo(DispatcherStatus.AVAILABLE);
        this.availableSince = Objects.requireNonNull(now, "now must not be null");
    }

    public void goOffline() {
        transitionTo(DispatcherStatus.OFFLINE);
        this.availableSince = null;
    }

    public void markBusy() {
        transitionTo(DispatcherStatus.BUSY);
        this.availableSince = null;
    }

    private void transitionTo(DispatcherStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStatusTransitionException(status, target);
        }
        this.status = target;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public DispatcherStatus getStatus() {
        return status;
    }

    public Instant getAvailableSince() {
        return availableSince;
    }

    public Long getVersion() {
        return version;
    }
}
