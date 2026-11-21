package pl.kubit.emergency_call_dispatcher.stats;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/api/stats")
    public CallStats stats() {
        return statsService.getStats();
    }
}
