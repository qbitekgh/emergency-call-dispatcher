package pl.kubit.emergency_call_dispatcher.dispatcher;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for managing dispatchers.
 */
@RestController
@RequestMapping("/api/dispatchers")
@RequiredArgsConstructor
public class DispatcherController {

    private final DispatcherService dispatcherService;

    /**
     * Returns 201 without a {@code Location} header: the API has no endpoint for a single dispatcher.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DispatcherResponse register(@Valid @RequestBody CreateDispatcherRequest request) {
        return DispatcherResponse.from(dispatcherService.register(request.name()));
    }

    @GetMapping
    public List<DispatcherResponse> list() {
        return dispatcherService.findAll().stream()
                .map(DispatcherResponse::from)
                .toList();
    }

    @PutMapping("/{id}/status")
    public DispatcherResponse changeStatus(@PathVariable Long id,
                                           @Valid @RequestBody ChangeDispatcherStatusRequest request) {
        return DispatcherResponse.from(dispatcherService.changeStatus(id, request.status()));
    }
}
