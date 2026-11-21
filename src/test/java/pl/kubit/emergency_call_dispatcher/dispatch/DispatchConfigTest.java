package pl.kubit.emergency_call_dispatcher.dispatch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that {@code dispatch.strategy} selects the right bean. Starts only {@link DispatchConfig},
 * not the whole application, so no database is needed.
 */
class DispatchConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DispatchConfig.class);

    @Test
    void shouldUsePriorityStrategy_whenPropertyIsNotSet() {
        contextRunner.run(context ->
                assertThat(context).getBean(QueueOrderingStrategy.class).isInstanceOf(PriorityThenFifoStrategy.class));
    }

    @Test
    void shouldUsePriorityStrategy_whenPropertyIsPriority() {
        contextRunner.withPropertyValues("dispatch.strategy=priority").run(context ->
                assertThat(context).getBean(QueueOrderingStrategy.class).isInstanceOf(PriorityThenFifoStrategy.class));
    }

    @Test
    void shouldUseFifoStrategy_whenPropertyIsFifo() {
        contextRunner.withPropertyValues("dispatch.strategy=fifo").run(context ->
                assertThat(context).getBean(QueueOrderingStrategy.class).isInstanceOf(FifoStrategy.class));
    }

    @Test
    void shouldFailToStart_whenPropertyHasUnknownValue() {
        contextRunner.withPropertyValues("dispatch.strategy=random").run(context ->
                assertThat(context).hasFailed());
    }
}
