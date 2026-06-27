package com.careerai.common.exception;

/**
 * Thrown when a requested domain resource cannot be located. Maps to HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Object id) {
        super("%s not found with id: %s".formatted(resource, id));
    }
}
