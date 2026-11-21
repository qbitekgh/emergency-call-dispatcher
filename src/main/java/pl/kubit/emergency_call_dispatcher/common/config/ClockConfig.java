package pl.kubit.emergency_call_dispatcher.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Single source of the current time. Inject {@link Clock} instead of calling {@code Instant.now()},
 * so tests can replace it with {@link Clock#fixed}.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
