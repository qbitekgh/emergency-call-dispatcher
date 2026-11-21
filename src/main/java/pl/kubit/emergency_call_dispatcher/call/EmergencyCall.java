package pl.kubit.emergency_call_dispatcher.call;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.kubit.emergency_call_dispatcher.common.IllegalStatusTransitionException;
import pl.kubit.emergency_call_dispatcher.dispatcher.Dispatcher;

import java.time.Instant;
import java.util.Objects;

/**
 * An incoming emergency call waiting for, or being handled by, a dispatcher.
 * <p>
 * State changes only through domain methods ({@link #assignTo}, {@link #close}, {@link #abandon}),
 * which validate the transition with {@link CallStatus#canTransitionTo}. The call also keeps its
 * dispatcher's status consistent: assigning makes the dispatcher BUSY, closing makes them AVAILABLE again.
 * <p>
 * Current time is passed in as a parameter instead of being read from a clock, so the entity stays
 * a plain object that is easy to test.
 * <p>
 * Lombok generates only getters. There is deliberately no {@code @Setter} (state changes only through
 * domain methods) and no {@code @Data} (its equals/hashCode over all fields would use the mutable,
 * database-generated id and could touch lazy relations).
 */
@Entity
@Table(name = "emergency_call")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // required by JPA; not for application code
public class EmergencyCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    private String callerNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CallType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority;

    @Column(nullable = false)
    private String location;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CallStatus status;

    @Column(nullable = false)
    private Instant receivedAt;

    private Instant assignedAt;

    /** Time the call ended: either closed by the dispatcher or abandoned by the caller. */
    private Instant closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_dispatcher_id")
    private Dispatcher assignedDispatcher;

    /** Optimistic locking: two concurrent transactions cannot both assign the same call. */
    @Version
    private Long version;

    public EmergencyCall(String callerNumber, CallType type, Priority priority,
                         String location, String description, Instant receivedAt) {
        this.callerNumber = Objects.requireNonNull(callerNumber, "callerNumber must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.priority = Objects.requireNonNull(priority, "priority must not be null");
        this.location = Objects.requireNonNull(location, "location must not be null");
        this.description = description;
        this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt must not be null");
        this.status = CallStatus.QUEUED;
    }

    public void assignTo(Dispatcher dispatcher, Instant now) {
        Objects.requireNonNull(dispatcher, "dispatcher must not be null");
        requireTransitionTo(CallStatus.ASSIGNED);
        dispatcher.markBusy(); // throws if the dispatcher is not AVAILABLE, before this call is modified
        this.status = CallStatus.ASSIGNED;
        this.assignedDispatcher = dispatcher;
        this.assignedAt = now;
    }

    public void close(Instant now) {
        requireTransitionTo(CallStatus.CLOSED);
        this.status = CallStatus.CLOSED;
        this.closedAt = now;
        assignedDispatcher.goAvailable(now);
    }

    public void abandon(Instant now) {
        requireTransitionTo(CallStatus.ABANDONED);
        this.status = CallStatus.ABANDONED;
        this.closedAt = now;
    }

    private void requireTransitionTo(CallStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStatusTransitionException(status, target);
        }
    }
}
