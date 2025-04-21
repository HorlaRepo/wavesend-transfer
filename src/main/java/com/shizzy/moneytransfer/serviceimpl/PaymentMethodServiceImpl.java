package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.model.PaymentMethod;
import com.shizzy.moneytransfer.repository.CountryPaymentMethodRepository;
import com.shizzy.moneytransfer.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentMethodServiceImpl  implements PaymentMethodService {

    private final CountryPaymentMethodRepository countryPaymentMethodRepository;

    @Override
    public ApiResponse<List<PaymentMethod>> findPaymentMethodsByCountryAcronym(String acronym) {
        List<PaymentMethod> paymentMethods = countryPaymentMethodRepository.findPaymentMethodsByCountryAcronym(acronym);
        return ApiResponse.<List<PaymentMethod>>builder()
                .success(true)
                .message("Payment methods retrieved successfully")
                .data(paymentMethods)
                .build();
    }
}
