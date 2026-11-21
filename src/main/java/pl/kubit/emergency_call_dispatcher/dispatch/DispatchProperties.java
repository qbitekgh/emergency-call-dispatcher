package pl.kubit.emergency_call_dispatcher.dispatch;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings under the {@code dispatch} prefix.
 * <p>
 * Binding to an enum means a typo such as {@code dispatch.strategy=fifoo} fails at startup
 * with a clear message instead of silently falling back to a default.
 */
@ConfigurationProperties("dispatch")
public record DispatchProperties(@DefaultValue("priority") Strategy strategy) {

    public enum Strategy {
        PRIORITY,
        FIFO
    }
}
