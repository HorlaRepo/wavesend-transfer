package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.model.PaymentMethod;
import com.shizzy.moneytransfer.repository.CountryPaymentMethodRepository;
import com.shizzy.moneytransfer.service.CountryPaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CountryPaymentMethodServiceImpl implements CountryPaymentMethodService {
    private final CountryPaymentMethodRepository paymentMethodRepository;

    @Override
    public ApiResponse<List<PaymentMethod>> findPaymentMethodsByCountryAcronym(String acronym) {
        List<PaymentMethod> paymentMethods =  paymentMethodRepository.findPaymentMethodsByCountryAcronym(acronym);

        return ApiResponse.<List<PaymentMethod>>builder()
                .data(paymentMethods)
                .message("Payment methods retrieved successfully")
                .success(true)
                .build();
    }
}
