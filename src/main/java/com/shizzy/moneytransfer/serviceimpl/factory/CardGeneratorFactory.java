package com.shizzy.moneytransfer.serviceimpl.factory;

import com.shizzy.moneytransfer.serviceimpl.strategy.CardNumberGenerator;
import com.shizzy.moneytransfer.serviceimpl.strategy.MasterCardNumberGenerator;
import com.shizzy.moneytransfer.serviceimpl.strategy.VisaCardNumberGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CardGeneratorFactory {
    private final Map<String, CardNumberGenerator> generators = new HashMap<>();

    @Autowired
    public CardGeneratorFactory(VisaCardNumberGenerator visaGenerator,
                                MasterCardNumberGenerator masterCardGenerator) {
        generators.put("VISA", visaGenerator);
        generators.put("MASTERCARD", masterCardGenerator);
    }

    public CardNumberGenerator getGenerator(String cardType) {
        CardNumberGenerator generator = generators.get(cardType.toUpperCase());
        if (generator == null) {
            throw new IllegalArgumentException("Unsupported card type: " + cardType);
        }
        return generator;
    }
}
