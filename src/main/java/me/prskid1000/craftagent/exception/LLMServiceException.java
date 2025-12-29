package me.prskid1000.craftagent.exception;

public class LLMServiceException extends RuntimeException {
	public LLMServiceException(String message) {
		super(message);
	}

	public LLMServiceException() {
		super();
	}

	public LLMServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
