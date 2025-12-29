package me.prskid1000.craftagent.event;

public class CustomEventException extends RuntimeException {
    public CustomEventException(String message, Exception cause) {
        super(message, cause);
    }
}
