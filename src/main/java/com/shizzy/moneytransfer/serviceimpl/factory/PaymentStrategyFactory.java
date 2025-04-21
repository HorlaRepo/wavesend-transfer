package com.shizzy.moneytransfer.serviceimpl.factory;

import com.shizzy.moneytransfer.service.payment.PaymentGatewayStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PaymentStrategyFactory {
    private final Map<String, PaymentGatewayStrategy> strategies;

    public PaymentStrategyFactory(List<PaymentGatewayStrategy> strategyList) {
        strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        s -> s.getClass().getAnnotation(Qualifier.class).value(),
                        s -> s
                ));
    }

    public PaymentGatewayStrategy getStrategy(String provider) {
        return strategies.getOrDefault(provider + "Strategy",
                strategies.get("defaultStrategy"));
    }
}
