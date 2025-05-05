package com.shizzy.moneytransfer.dto;

/**
 * Enum representing the possible intents from user messages
 */
public enum TransactionIntent {
    TRANSFER,
    SCHEDULED_TRANSFER,
    WITHDRAWAL,
    CHECK_BALANCE,
    LIST_BENEFICIARIES,
    TRANSACTION_HISTORY,
    HELP,
    UNKNOWN, 
    OUT_OF_SCOPE
}