package com.shizzy.moneytransfer.serviceimpl.strategy;

import com.shizzy.moneytransfer.util.CardValidationUtils;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class VisaCardNumberGenerator implements CardNumberGenerator {
    private final Random random = new Random();

    @Override
    public String generateCardNumber() {
        String prefix = "4";
        StringBuilder cardNumber = new StringBuilder(prefix);
        while (cardNumber.length() < 15) {
            cardNumber.append(random.nextInt(10));
        }
        cardNumber.append(CardValidationUtils.calculateLuhnCheckDigit(cardNumber.toString()));
        return cardNumber.toString();
    }
}