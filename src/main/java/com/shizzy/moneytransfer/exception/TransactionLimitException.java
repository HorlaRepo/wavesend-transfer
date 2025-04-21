package com.shizzy.moneytransfer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class TransactionLimitException extends RuntimeException{
    public TransactionLimitException(String message){
        super(message);
    }
}
