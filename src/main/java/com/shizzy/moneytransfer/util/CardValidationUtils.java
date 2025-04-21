package com.shizzy.moneytransfer.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class CardValidationUtils {
    public static int calculateLuhnCheckDigit(String cardNumberWithoutCheckDigit) {
        int sum = 0;
        boolean alternate = true;

        for (int i = cardNumberWithoutCheckDigit.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cardNumberWithoutCheckDigit.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (10 - (sum % 10)) % 10;
    }

    public static String generateExpiryDate() {
        LocalDate expiryDate = LocalDate.now().plusYears(5);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");
        return expiryDate.format(formatter);
    }

    public static String generateCVV(Random random) {
        int cvv = random.nextInt(900) + 100;
        return String.valueOf(cvv);
    }
}
