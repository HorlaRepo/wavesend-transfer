package com.shizzy.moneytransfer.enums;

/**
 * Represents the different verification levels for user accounts.
 */
public enum VerificationLevel {
    UNVERIFIED(0),        // New account with no verification
    EMAIL_VERIFIED(1),    // Email verification completed
    ID_VERIFIED(2),       // ID document verified
    FULLY_VERIFIED(3);    // Both ID and address verification completed
    
    private final int level;
    
    VerificationLevel(int level) {
        this.level = level;
    }
    
    public int getLevel() {
        return level;
    }
    
    /**
     * Check if this level is higher than or equal to the provided level
     */
    public boolean isAtLeast(VerificationLevel other) {
        return this.level >= other.level;
    }
}