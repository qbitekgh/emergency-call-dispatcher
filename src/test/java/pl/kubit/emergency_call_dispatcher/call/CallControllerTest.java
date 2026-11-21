package pl.kubit.emergency_call_dispatcher.call;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import pl.kubit.emergency_call_dispatcher.common.IllegalStatusTransitionException;
import pl.kubit.emergency_call_dispatcher.common.ResourceNotFoundException;
import pl.kubit.emergency_call_dispatcher.dispatcher.Dispatcher;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer only: starts {@link CallController} and the exception handler with a mocked {@link CallService}.
 * Checks HTTP status codes, JSON format, validation and {@code ProblemDetail} errors; no database is used.
 */
@WebMvcTest(CallController.class)
class CallControllerTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    private static final String VALID_BODY = """
            {
              "callerNumber": "+48123456789",
              "type": "FIRE",
              "priority": "CRITICAL",
              "location": "Main St 1",
              "description": "Kitchen on fire"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CallService callService;

    @Test
    void shouldReturn201WithLocationAndCall_whenRequestIsValid() throws Exception {
        when(callService.receive(any())).thenReturn(callWithId(1L));

        mockMvc.perform(post("/api/calls").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/calls/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.priority").value("CRITICAL"))
                .andExpect(jsonPath("$.receivedAt").value("2026-01-01T10:00:00Z"))
                .andExpect(jsonPath("$.assignedDispatcherId").isEmpty());

        verify(callService).receive(new ReceiveCallCommand(
                "+48123456789", CallType.FIRE, Priority.CRITICAL, "Main St 1", "Kitchen on fire"));
    }

    @Test
    void shouldReturn400WithFieldErrors_whenBodyIsInvalid() throws Exception {
        String body = """
                {
                  "callerNumber": "12ab",
                  "priority": "HIGH",
                  "location": " "
                }
                """;

        mockMvc.perform(post("/api/calls").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.callerNumber").exists())
                .andExpect(jsonPath("$.errors.type").exists())
                .andExpect(jsonPath("$.errors.location").exists());

        verify(callService, never()).receive(any());
    }

    @Test
    void shouldReturn400_whenEnumValueIsUnknown() throws Exception {
        String body = VALID_BODY.replace("FIRE", "SPACE");

        mockMvc.perform(post("/api/calls").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        verify(callService, never()).receive(any());
    }

    @Test
    void shouldReturnPageOfCalls_whenListingWithStatusFilter() throws Exception {
        PageRequest expectedRequest = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "receivedAt"));
        when(callService.findAll(CallStatus.QUEUED, expectedRequest))
                .thenReturn(new PageImpl<>(List.of(callWithId(7L)), expectedRequest, 6));

        mockMvc.perform(get("/api/calls").param("status", "QUEUED").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void shouldUseFirstPageOfTwentyWithoutFilter_whenNoParametersAreGiven() throws Exception {
        PageRequest expectedRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "receivedAt"));
        when(callService.findAll(null, expectedRequest)).thenReturn(new PageImpl<>(List.of(), expectedRequest, 0));

        mockMvc.perform(get("/api/calls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldReturn400_whenPageSizeIsTooLarge() throws Exception {
        mockMvc.perform(get("/api/calls").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void shouldReturn400_whenStatusFilterIsUnknown() throws Exception {
        mockMvc.perform(get("/api/calls").param("status", "LOST"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void shouldReturnCall_whenCallExists() throws Exception {
        when(callService.findById(1L)).thenReturn(callWithId(1L));

        mockMvc.perform(get("/api/calls/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.location").value("Main St 1"));
    }

    @Test
    void shouldReturn404ProblemDetail_whenCallDoesNotExist() throws Exception {
        when(callService.findById(99L)).thenThrow(new ResourceNotFoundException("Call", 99L));

        mockMvc.perform(get("/api/calls/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("Call with id 99 not found"));
    }

    @Test
    void shouldReturnClosedCallWithDispatcher_whenCallIsClosed() throws Exception {
        Dispatcher dispatcher = new Dispatcher("Anna");
        ReflectionTestUtils.setField(dispatcher, "id", 3L);
        dispatcher.goAvailable(T0);
        EmergencyCall call = callWithId(1L);
        call.assignTo(dispatcher, T0.plusSeconds(5));
        call.close(T0.plusSeconds(60));
        when(callService.close(1L)).thenReturn(call);

        mockMvc.perform(post("/api/calls/1/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.assignedDispatcherId").value(3))
                .andExpect(jsonPath("$.closedAt").value("2026-01-01T10:01:00Z"));
    }

    @Test
    void shouldReturn409ProblemDetail_whenTransitionIsIllegal() throws Exception {
        when(callService.close(1L))
                .thenThrow(new IllegalStatusTransitionException(CallStatus.QUEUED, CallStatus.CLOSED));

        mockMvc.perform(post("/api/calls/1/close"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Illegal status transition"))
                .andExpect(jsonPath("$.detail").value("Illegal status transition for CallStatus: QUEUED -> CLOSED"));
    }

    @Test
    void shouldReturn409ProblemDetail_whenCallWasModifiedConcurrently() throws Exception {
        when(callService.close(1L))
                .thenThrow(new ObjectOptimisticLockingFailureException(EmergencyCall.class, 1L));

        mockMvc.perform(post("/api/calls/1/close"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Concurrent modification"));
    }

    @Test
    void shouldReturnAbandonedCall_whenCallIsAbandoned() throws Exception {
        EmergencyCall call = callWithId(1L);
        call.abandon(T0.plusSeconds(30));
        when(callService.abandon(1L)).thenReturn(call);

        mockMvc.perform(post("/api/calls/1/abandon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABANDONED"));
    }

    /**
     * The id is normally generated by the database; in a web-layer test it is set by reflection.
     */
    private static EmergencyCall callWithId(Long id) {
        EmergencyCall call = new EmergencyCall("+48123456789", CallType.FIRE, Priority.CRITICAL,
                "Main St 1", "Kitchen on fire", T0);
        ReflectionTestUtils.setField(call, "id", id);
        return call;
    }
}
