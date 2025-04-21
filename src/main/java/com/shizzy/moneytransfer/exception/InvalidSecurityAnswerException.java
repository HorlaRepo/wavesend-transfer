package com.shizzy.moneytransfer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidSecurityAnswerException extends RuntimeException {
    public InvalidSecurityAnswerException(String message) {
        super(message);
    }
}
