package com.shizzy.moneytransfer.serviceimpl.strategy;

import com.shizzy.moneytransfer.util.CardValidationUtils;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class MasterCardNumberGenerator implements CardNumberGenerator {
    private final Random random = new Random();
    private final int[] masterCardPrefixes = {51, 52, 53, 54, 55};

    @Override
    public String generateCardNumber() {
        String prefix = String.valueOf(masterCardPrefixes[random.nextInt(masterCardPrefixes.length)]);
        StringBuilder cardNumber = new StringBuilder(prefix);
        while (cardNumber.length() < 15) {
            cardNumber.append(random.nextInt(10));
        }
        cardNumber.append(CardValidationUtils.calculateLuhnCheckDigit(cardNumber.toString()));
        return cardNumber.toString();
    }
}
