package pl.kubit.emergency_call_dispatcher.call;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.kubit.emergency_call_dispatcher.common.PageResponse;

import java.net.URI;

/**
 * REST endpoints for emergency calls. The controller only translates HTTP to service calls
 * and entities to DTOs; all business rules stay in the service and the domain model.
 */
@RestController
@RequestMapping("/api/calls")
@RequiredArgsConstructor
public class CallController {

    /** Newest calls first on the list endpoint. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "receivedAt");

    private final CallService callService;

    @PostMapping
    public ResponseEntity<CallResponse> receive(@Valid @RequestBody CreateCallRequest request) {
        CallResponse response = CallResponse.from(callService.receive(request.toCommand()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    /**
     * @param status optional filter; without it calls in every status are returned
     */
    @GetMapping
    public PageResponse<CallResponse> list(@RequestParam(required = false) CallStatus status,
                                           @RequestParam(defaultValue = "0") @Min(0) int page,
                                           @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        PageRequest pageRequest = PageRequest.of(page, size, NEWEST_FIRST);
        return PageResponse.from(callService.findAll(status, pageRequest), CallResponse::from);
    }

    @GetMapping("/{id}")
    public CallResponse get(@PathVariable Long id) {
        return CallResponse.from(callService.findById(id));
    }

    @PostMapping("/{id}/close")
    public CallResponse close(@PathVariable Long id) {
        return CallResponse.from(callService.close(id));
    }

    @PostMapping("/{id}/abandon")
    public CallResponse abandon(@PathVariable Long id) {
        return CallResponse.from(callService.abandon(id));
    }
}
