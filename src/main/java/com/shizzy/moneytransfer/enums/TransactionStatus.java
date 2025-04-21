package com.shizzy.moneytransfer.enums;

import lombok.Getter;

@Getter
public enum TransactionStatus {
    SUCCESS("SUCCESS"),
    FAILED("FAILED"),
    PENDING("PENDING")
    ;

    private final String value;

    TransactionStatus(String value) {
        this.value = value;
    }
}
