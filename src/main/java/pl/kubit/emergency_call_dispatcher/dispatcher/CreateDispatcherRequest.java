package pl.kubit.emergency_call_dispatcher.dispatcher;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/dispatchers}. A new dispatcher always starts OFFLINE, so only the name is needed.
 */
public record CreateDispatcherRequest(
        @NotBlank
        @Size(max = 100)
        String name) {
}
