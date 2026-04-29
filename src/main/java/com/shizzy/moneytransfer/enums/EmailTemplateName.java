package com.shizzy.moneytransfer.enums;

import lombok.Getter;

@Getter
public enum EmailTemplateName {

    DEPOSIT("deposit_success"),
    WITHDRAWAL("withdrawal_success"),
    CREDIT_TRANSFER("credit_transfer"),
    DEBIT_TRANSFER("debit_transfer"),
    SCHEDULED_TRANSFER("scheduled_transfer"),
    EXECUTED_TRANSFER("executed_transfer"),
    CANCELLED_TRANSFER("cancelled_transfer"),
    FAILED_TRANSFER("failed_transfer"),
    OTP_VERIFICATION("verification");

    private final String name;

    EmailTemplateName(String name) {
        this.name = name;
    }
}
