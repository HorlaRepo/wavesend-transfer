package com.shizzy.moneytransfer.serviceimpl.builder;

import com.shizzy.moneytransfer.model.BankAccount;

public interface BankAccountBuilder {
    BankAccount buildBankAccount(Object dto, String createdBy);
}
