package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.model.PaymentMethod;

import java.util.List;

public interface CountryPaymentMethodService {
    ApiResponse<List<PaymentMethod>> findPaymentMethodsByCountryAcronym(String acronym);
}
