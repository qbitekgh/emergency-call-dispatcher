package pl.kubit.emergency_call_dispatcher.dispatcher;

import jakarta.validation.constraints.NotNull;

/**
 * Body of {@code PUT /api/dispatchers/{id}/status}. Only AVAILABLE and OFFLINE are accepted;
 * BUSY is rejected by {@link DispatcherService#changeStatus} because it is set only by assigning a call.
 */
public record ChangeDispatcherStatusRequest(
        @NotNull
        DispatcherStatus status) {
}
