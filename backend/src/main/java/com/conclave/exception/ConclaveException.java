package com.conclave.exception;

/**
 * Common base exception for all custom runtime validation failures in Conclave.
 */
public class ConclaveException extends RuntimeException {
    public ConclaveException(String message) {
        super(message);
    }

    public ConclaveException(String message, Throwable cause) {
        super(message, cause);
    }
}
