package pl.kubit.emergency_call_dispatcher.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * One page of results returned by the API.
 * <p>
 * Spring Data's {@link Page} is not returned directly: its JSON shape is an internal detail of the library
 * and would also expose entities. This record keeps the response format small and under our control.
 *
 * @param content       items on this page
 * @param page          zero-based page number
 * @param size          requested page size
 * @param totalElements number of items on all pages
 * @param totalPages    number of pages
 * @param <T>           type of a single item, e.g. a response DTO
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    /**
     * Builds a response from a Spring Data page, converting each element with {@code mapper}
     * (for example an entity into its DTO).
     */
    public static <E, T> PageResponse<T> from(Page<E> page, Function<? super E, ? extends T> mapper) {
        List<T> content = page.getContent().stream()
                .<T>map(mapper)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
