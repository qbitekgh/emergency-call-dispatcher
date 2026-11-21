package pl.kubit.emergency_call_dispatcher.dispatch;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only view of the calls waiting for a dispatcher.
 */
@RestController
@RequiredArgsConstructor
public class QueueController {

    private final DispatchService dispatchService;

    @GetMapping("/api/queue")
    public QueueResponse queue() {
        return QueueResponse.from(dispatchService.queue());
    }
}
