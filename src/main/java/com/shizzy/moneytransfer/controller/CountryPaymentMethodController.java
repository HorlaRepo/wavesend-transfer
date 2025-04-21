package com.shizzy.moneytransfer.controller;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.model.PaymentMethod;
import com.shizzy.moneytransfer.service.CountryPaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("country-payment-method")
@RequiredArgsConstructor
public class CountryPaymentMethodController {

    private final CountryPaymentMethodService countryPaymentMethodService;

    @GetMapping("/{acronym}")
    public ApiResponse<List<PaymentMethod>> getPaymentMethodsByCountryAcronym(@PathVariable String acronym) {
        return countryPaymentMethodService.findPaymentMethodsByCountryAcronym(acronym);
    }
}
