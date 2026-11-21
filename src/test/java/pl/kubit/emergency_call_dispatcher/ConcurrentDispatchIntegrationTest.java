package pl.kubit.emergency_call_dispatcher;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.kubit.emergency_call_dispatcher.call.CallService;
import pl.kubit.emergency_call_dispatcher.call.CallStatus;
import pl.kubit.emergency_call_dispatcher.call.CallType;
import pl.kubit.emergency_call_dispatcher.call.EmergencyCall;
import pl.kubit.emergency_call_dispatcher.call.EmergencyCallRepository;
import pl.kubit.emergency_call_dispatcher.call.Priority;
import pl.kubit.emergency_call_dispatcher.call.ReceiveCallCommand;
import pl.kubit.emergency_call_dispatcher.dispatcher.Dispatcher;
import pl.kubit.emergency_call_dispatcher.dispatcher.DispatcherRepository;
import pl.kubit.emergency_call_dispatcher.dispatcher.DispatcherStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Many calls arriving at the same moment: every request runs in its own thread and transaction,
 * and they all compete for the same few available dispatchers.
 */
class ConcurrentDispatchIntegrationTest extends IntegrationTest {

    private static final int CALLS = 20;
    private static final int DISPATCHERS = 3;

    @Autowired
    private CallService callService;

    @Autowired
    private EmergencyCallRepository callRepository;

    @Autowired
    private DispatcherRepository dispatcherRepository;

    @Test
    void shouldAcceptEveryCallAndGiveEachDispatcherOneCall_whenCallsArriveConcurrently() throws Exception {
        for (int i = 1; i <= DISPATCHERS; i++) {
            availableDispatcher("Dispatcher " + i);
        }
        ReceiveCallCommand command =
                new ReceiveCallCommand("+48123456789", CallType.FIRE, Priority.HIGH, "Main St 1", null);

        // All threads wait at the latch and start together, to make collisions as likely as possible.
        CountDownLatch start = new CountDownLatch(1);
        List<Future<EmergencyCall>> results = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(CALLS)) {
            for (int i = 0; i < CALLS; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return callService.receive(command);
                }));
            }
            start.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }

        // Future.get() rethrows any exception from the thread, e.g. an optimistic locking failure.
        for (Future<EmergencyCall> result : results) {
            assertThat(result.get().getId()).isNotNull();
        }
        assertThat(callRepository.count()).isEqualTo(CALLS);

        List<EmergencyCall> assigned = callRepository.findByStatus(CallStatus.ASSIGNED);
        Map<Long, Long> callsPerDispatcher = assigned.stream()
                .collect(Collectors.groupingBy(call -> call.getAssignedDispatcher().getId(), Collectors.counting()));
        assertThat(callsPerDispatcher).hasSize(DISPATCHERS).allSatisfy((dispatcherId, count) -> assertThat(count).isOne());
        assertThat(callRepository.findByStatus(CallStatus.QUEUED)).hasSize(CALLS - DISPATCHERS);
        assertThat(dispatcherRepository.findAll()).extracting(Dispatcher::getStatus).containsOnly(DispatcherStatus.BUSY);
    }
}
