package com.shizzy.moneytransfer.controller;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.model.PaymentMethod;
import com.shizzy.moneytransfer.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payment-methods")
public class PaymentMethodsController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping("/countryAcronym")
    public ApiResponse<List<PaymentMethod>> getPaymentMethodsByCountryAcronym(String countryAcronym) {
        return paymentMethodService.findPaymentMethodsByCountryAcronym(countryAcronym);
    }
}
