package com.shizzy.moneytransfer.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BankAccountValidator.class)
public @interface ValidBankAccount {
    String message() default "Invalid bank account details";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}