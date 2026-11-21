package pl.kubit.emergency_call_dispatcher.common;

/**
 * Thrown when a resource with the requested id does not exist; mapped to HTTP 404 at the API boundary.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Long id) {
        super("%s with id %d not found".formatted(resource, id));
    }
}
