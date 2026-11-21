package pl.kubit.emergency_call_dispatcher;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks the {@code spring.web.locale} setting: built-in Bean Validation messages are always English,
 * the same language as the application's own messages.
 */
class ValidationLanguageIntegrationTest extends IntegrationTest {

    @Test
    void shouldReturnEnglishValidationMessages_whenClientAsksForAnotherLanguage() throws Exception {
        mockMvc.perform(post("/api/calls")
                        .header("Accept-Language", "pl-PL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"callerNumber": "+48123456789", "type": "FIRE", "priority": "HIGH", "location": " "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.location").value("must not be blank"));
    }
}
