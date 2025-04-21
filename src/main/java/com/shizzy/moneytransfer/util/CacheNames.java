package com.shizzy.moneytransfer.util;

public class CacheNames {
    public static final String TRANSACTIONS = "transactions";
    public static final String SINGLE_TRANSACTION = "singleTransaction";
    public static final String ALL_USER_TRANSACTION = "allUserTransaction";
    public static final String SEARCH_RESULT = "searchResult";
    public static final String WALLETS = "wallets";
    public static final String USERS = "users";
    public static final String OTP_CACHE = "otpCache";
    public static final String PENDING_TRANSFERS = "pendingTransfersCache";
    public static final String PENDING_WITHDRAWALS = "pendingWithdrawalsCache";
    public static final String PENDING_SCHEDULED_TRANSFERS = "pendingScheduledTransfersCache";
    
    // Beneficiaries caches
    public static final String USER_BENEFICIARIES = "userBeneficiaries";
    public static final String SINGLE_BENEFICIARY = "singleBeneficiary";


    // Scheduled transfers caches
    public static final String SCHEDULED_TRANSFERS = "scheduledTransfers";
    public static final String SINGLE_SCHEDULED_TRANSFER = "singleScheduledTransfer";
    public static final String USER_SCHEDULED_TRANSFERS = "userScheduledTransfers";
    public static final String RECURRING_SERIES = "recurringSeriesTransfers";
    
    // Prevent instantiation
    private CacheNames() {}
}