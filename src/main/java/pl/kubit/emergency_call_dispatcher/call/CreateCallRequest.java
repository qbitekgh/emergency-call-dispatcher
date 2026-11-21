package pl.kubit.emergency_call_dispatcher.call;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/calls}. Validated with Bean Validation before reaching the service.
 * Limits match the columns in {@code V1__init.sql}.
 */
public record CreateCallRequest(
        @NotBlank
        @Pattern(regexp = "\\+?\\d{9,15}", message = "must be 9-15 digits, optionally starting with +")
        String callerNumber,

        @NotNull
        CallType type,

        @NotNull
        Priority priority,

        @NotBlank
        @Size(max = 255)
        String location,

        @Size(max = 1000)
        String description) {

    /**
     * Converts the web request into the service's input, so {@link CallService} does not depend on the web layer.
     */
    public ReceiveCallCommand toCommand() {
        return new ReceiveCallCommand(callerNumber, type, priority, location, description);
    }
}
