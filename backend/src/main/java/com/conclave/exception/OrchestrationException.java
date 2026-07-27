package com.conclave.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class OrchestrationException extends RuntimeException {
    
    public OrchestrationException(String message) {
        super(message);
    }

    public OrchestrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
