package com.dehold.contentmanager.exception;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }

    public static EntityNotFoundException of(String entityType, String id) {
        return new EntityNotFoundException(String.format("The entity %s with id %s does not exist", id, entityType));
    }
}
