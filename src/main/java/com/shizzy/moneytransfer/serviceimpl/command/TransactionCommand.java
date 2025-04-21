package com.shizzy.moneytransfer.serviceimpl.command;

import com.shizzy.moneytransfer.api.ApiResponse;

public interface TransactionCommand<T> {
    ApiResponse<T> execute();
}
