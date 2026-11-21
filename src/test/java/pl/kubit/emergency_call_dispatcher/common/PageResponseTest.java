package pl.kubit.emergency_call_dispatcher.common;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void shouldMapContentAndCopyPageInfo_whenBuiltFromSpringPage() {
        var page = new PageImpl<>(List.of("a", "bb"), PageRequest.of(2, 2), 7);

        PageResponse<Integer> response = PageResponse.from(page, String::length);

        assertThat(response.content()).containsExactly(1, 2);
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(7);
        assertThat(response.totalPages()).isEqualTo(4);
    }

    @Test
    void shouldReturnEmptyContent_whenPageIsEmpty() {
        var page = new PageImpl<String>(List.of(), PageRequest.of(0, 20), 0);

        PageResponse<Integer> response = PageResponse.from(page, String::length);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
    }
}
