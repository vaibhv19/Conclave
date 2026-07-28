package com.conclave.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class EmailAlreadyExistsException extends ConclaveException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
