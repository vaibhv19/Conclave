package com.conclave.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidMappingException extends ConclaveException {
    public InvalidMappingException(String message) {
        super(message);
    }
}
