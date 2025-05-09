package com.shizzy.moneytransfer.enums;

public enum RefundImpactType {
    TRANSFER,        // Transfer reduced refundable amount
    REFUND,          // Refund processed
    ADJUSTMENT,      // Manual adjustment
    REVERSAL,        // Reversed a previous impact
    RECONCILIATION   // System reconciliation
}