package pl.kubit.emergency_call_dispatcher.dispatch;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creates the {@link QueueOrderingStrategy} selected by {@code dispatch.strategy}.
 * <p>
 * The rest of the application depends only on the interface (Dependency Inversion), so adding
 * a new ordering means a new class and one new {@code case} here; nothing else changes (Open/Closed).
 * The {@code switch} has no {@code default}, so the compiler reports a missing case when a new
 * {@link DispatchProperties.Strategy} constant is added.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DispatchProperties.class)
public class DispatchConfig {

    @Bean
    QueueOrderingStrategy queueOrderingStrategy(DispatchProperties properties) {
        return switch (properties.strategy()) {
            case PRIORITY -> new PriorityThenFifoStrategy();
            case FIFO -> new FifoStrategy();
        };
    }
}
