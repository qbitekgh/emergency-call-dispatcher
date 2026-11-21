package pl.kubit.emergency_call_dispatcher.dispatcher;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import pl.kubit.emergency_call_dispatcher.common.IllegalStatusTransitionException;
import pl.kubit.emergency_call_dispatcher.common.ResourceNotFoundException;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer only: {@link DispatcherController} and the exception handler with a mocked {@link DispatcherService}.
 */
@WebMvcTest(DispatcherController.class)
class DispatcherControllerTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DispatcherService dispatcherService;

    @Test
    void shouldReturn201WithOfflineDispatcher_whenRegistered() throws Exception {
        when(dispatcherService.register("Anna")).thenReturn(dispatcherWithId(1L, "Anna"));

        mockMvc.perform(post("/api/dispatchers").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Anna" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Anna"))
                .andExpect(jsonPath("$.status").value("OFFLINE"))
                .andExpect(jsonPath("$.availableSince").isEmpty());
    }

    @Test
    void shouldReturn400WithFieldError_whenNameIsBlank() throws Exception {
        mockMvc.perform(post("/api/dispatchers").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.name").exists());

        verify(dispatcherService, never()).register(anyString());
    }

    @Test
    void shouldReturnAllDispatchers_whenListed() throws Exception {
        when(dispatcherService.findAll())
                .thenReturn(List.of(dispatcherWithId(1L, "Anna"), dispatcherWithId(2L, "Piotr")));

        mockMvc.perform(get("/api/dispatchers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Anna"))
                .andExpect(jsonPath("$[1].name").value("Piotr"));
    }

    @Test
    void shouldReturnAvailableDispatcher_whenStatusIsChangedToAvailable() throws Exception {
        Dispatcher dispatcher = dispatcherWithId(1L, "Anna");
        dispatcher.goAvailable(T0);
        when(dispatcherService.changeStatus(1L, DispatcherStatus.AVAILABLE)).thenReturn(dispatcher);

        mockMvc.perform(put("/api/dispatchers/1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "AVAILABLE" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.availableSince").value("2026-01-01T10:00:00Z"));
    }

    @Test
    void shouldReturn400_whenStatusIsMissing() throws Exception {
        mockMvc.perform(put("/api/dispatchers/1/status").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status").exists());

        verify(dispatcherService, never()).changeStatus(any(), any());
    }

    @Test
    void shouldReturn400ProblemDetail_whenBusyIsRequestedManually() throws Exception {
        when(dispatcherService.changeStatus(1L, DispatcherStatus.BUSY))
                .thenThrow(new IllegalArgumentException("BUSY is set automatically when a call is assigned"));

        mockMvc.perform(put("/api/dispatchers/1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "BUSY" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("BUSY is set automatically when a call is assigned"));
    }

    @Test
    void shouldReturn404ProblemDetail_whenDispatcherDoesNotExist() throws Exception {
        when(dispatcherService.changeStatus(99L, DispatcherStatus.AVAILABLE))
                .thenThrow(new ResourceNotFoundException("Dispatcher", 99L));

        mockMvc.perform(put("/api/dispatchers/99/status").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "AVAILABLE" }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Dispatcher with id 99 not found"));
    }

    @Test
    void shouldReturn409ProblemDetail_whenTransitionIsIllegal() throws Exception {
        when(dispatcherService.changeStatus(1L, DispatcherStatus.OFFLINE))
                .thenThrow(new IllegalStatusTransitionException(DispatcherStatus.BUSY, DispatcherStatus.OFFLINE));

        mockMvc.perform(put("/api/dispatchers/1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "OFFLINE" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Illegal status transition"));
    }

    private static Dispatcher dispatcherWithId(Long id, String name) {
        Dispatcher dispatcher = new Dispatcher(name);
        ReflectionTestUtils.setField(dispatcher, "id", id);
        return dispatcher;
    }
}
