package com.shizzy.moneytransfer.validators;

import com.shizzy.moneytransfer.dto.AddBankAccountRequest;
import com.shizzy.moneytransfer.dto.AfricaBankAccountDto;
import com.shizzy.moneytransfer.dto.EUBankAccountDto;
import com.shizzy.moneytransfer.dto.USBankAccountDto;
import com.shizzy.moneytransfer.enums.Region;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BankAccountValidator implements ConstraintValidator<ValidBankAccount, AddBankAccountRequest> {

    @Override
    public boolean isValid(AddBankAccountRequest request, ConstraintValidatorContext context) {

        context.disableDefaultConstraintViolation();

        Region region = request.getRegion();
        Object details = request.getBankAccountDetails();

        // Check if region and bankAccountDetails are provided
        if (region == null || details == null) {
            context.buildConstraintViolationWithTemplate("Region and bank account details are required")
                    .addConstraintViolation();
            return false;
        }

        // Validate based on region by casting to the appropriate DTO
        return switch (region) {
            case AFRICA -> {
                if (!(details instanceof AfricaBankAccountDto)) {
                    context.buildConstraintViolationWithTemplate("Invalid bank account details format for AFRICA region")
                            .addConstraintViolation();
                    yield false;
                }
                yield validateAfricaDto((AfricaBankAccountDto) details, context);
            }
            case EU -> {
                if (!(details instanceof EUBankAccountDto)) {
                    context.buildConstraintViolationWithTemplate("Invalid bank account details format for EU region")
                            .addConstraintViolation();
                    yield false;
                }
                yield validateEUDto((EUBankAccountDto) details, context);
            }
            case US -> {
                if (!(details instanceof USBankAccountDto)) {
                    context.buildConstraintViolationWithTemplate("Invalid bank account details format for US region")
                            .addConstraintViolation();
                    yield false;
                }
                yield validateUSDto((USBankAccountDto) details, context);
            }
            default -> false; // Unsupported region
        };

    }

    /**
     * Validates Africa-specific bank account details.
     */
    private boolean validateAfricaDto(AfricaBankAccountDto dto, ConstraintValidatorContext context) {
        boolean isValid = true;

        // Required fields for Africa
        if (dto.getBankName() == null || dto.getBankName().trim().isEmpty()) {
            addViolation(context, "bankName", "bankName is required for AFRICA");
            isValid = false;
        }
        if (dto.getAccountNumber() == null || dto.getAccountNumber().trim().isEmpty()) {
            addViolation(context, "accountNumber", "accountNumber is required for AFRICA");
            isValid = false;
        }
        if (dto.getAccountName() == null || dto.getAccountName().trim().isEmpty()) {
            addViolation(context, "accountName", "accountName is required for AFRICA");
            isValid = false;
        }
        if (dto.getCurrency() == null || dto.getCurrency().trim().isEmpty()) {
            addViolation(context, "currency", "currency is required for AFRICA");
            isValid = false;
        }
        if (dto.getBankCountry() == null || dto.getBankCountry().trim().isEmpty()) {
            addViolation(context, "bankCountry", "bankCountry is required for AFRICA");
            isValid = false;
        }
        if (dto.getBankCode() == null || dto.getBankCode().trim().isEmpty()) {
            addViolation(context, "bankCode", "bankCode is required for AFRICA");
            isValid = false;
        }
        if (dto.getPaymentMethod() == null || dto.getPaymentMethod().trim().isEmpty()) {
            addViolation(context, "paymentMethod", "paymentMethod is required for AFRICA");
            isValid = false;
        }

        // Numeric validation
        if (!isNumeric(dto.getAccountNumber())) {
            addViolation(context, "accountNumber", "accountNumber must be numeric");
            isValid = false;
        }

        return isValid;
    }

    /**
     * Validates EU-specific bank account details.
     */
    private boolean validateEUDto(EUBankAccountDto dto, ConstraintValidatorContext context) {
        boolean isValid = true;
        // Required fields for EU
        if (dto.getBankName() == null || dto.getBankName().trim().isEmpty()) {
            addViolation(context, "bankName", "bankName is required for EU");
            isValid = false;
        }
        if (dto.getAccountNumber() == null || dto.getAccountNumber().trim().isEmpty()) {
            addViolation(context, "accountNumber", "accountNumber is required for EU");
            isValid = false;
        }
        if (dto.getAccountType() == null || dto.getAccountType().trim().isEmpty()) {
            addViolation(context, "accountType", "accountType is required for EU");
            isValid = false;
        }
        if (dto.getCurrency() == null || dto.getCurrency().trim().isEmpty()) {
            addViolation(context, "currency", "currency is required for EU");
            isValid = false;
        }
        if (dto.getBankCountry() == null || dto.getBankCountry().trim().isEmpty()) {
            addViolation(context, "bankCountry", "bankCountry is required for EU");
            isValid = false;
        }
        if (dto.getSwiftCode() == null || dto.getSwiftCode().trim().isEmpty()) {
            addViolation(context, "swiftCode", "swiftCode is required for EU");
            isValid = false;
        }
        if (dto.getRoutingNumber() == null || dto.getRoutingNumber().trim().isEmpty()) {
            addViolation(context, "routingNumber", "routingNumber is required for EU");
            isValid = false;
        }
        if (dto.getBeneficiaryName() == null || dto.getBeneficiaryName().trim().isEmpty()) {
            addViolation(context, "beneficiaryName", "beneficiaryName is required for EU");
            isValid = false;
        }
        if (dto.getBeneficiaryAddress() == null || dto.getBeneficiaryAddress().trim().isEmpty()) {
            addViolation(context, "beneficiaryAddress", "beneficiaryAddress is required for EU");
            isValid = false;
        }
        if (dto.getBeneficiaryCountry() == null || dto.getBeneficiaryCountry().trim().isEmpty()) {
            addViolation(context, "beneficiaryCountry", "beneficiaryCountry is required for EU");
            isValid = false;
        }
        if (dto.getPostalCode() == null || dto.getPostalCode().trim().isEmpty()) {
            addViolation(context, "postalCode", "postalCode is required for EU");
            isValid = false;
        }
        if (dto.getStreetNumber() == null || dto.getStreetNumber().trim().isEmpty()) {
            addViolation(context, "streetNumber", "streetNumber is required for EU");
            isValid = false;
        }
        if (dto.getStreetName() == null || dto.getStreetName().trim().isEmpty()) {
            addViolation(context, "streetName", "streetName is required for EU");
            isValid = false;
        }
        if (dto.getCity() == null || dto.getCity().trim().isEmpty()) {
            addViolation(context, "city", "city is required for EU");
            isValid = false;
        }
//        if (dto.getP == null || dto.getPaymentMethod().trim().isEmpty()) {
//            addViolation(context, "paymentMethod", "paymentMethod is required for EU");
//            return false;
//        }

        // Numeric validation
        if (!isNumeric(dto.getAccountNumber())) {
            addViolation(context, "accountNumber", "accountNumber must be numeric");
            isValid = false;
        }
        if (!isNumeric(dto.getRoutingNumber())) {
            addViolation(context, "routingNumber", "routingNumber must be numeric");
            isValid = false;
        }
        if (!isNumeric(dto.getPostalCode())) {
            addViolation(context, "postalCode", "postalCode must be numeric");
            isValid = false;
        }

        return isValid;
    }

    /**
     * Validates US-specific bank account details.
     */
    private boolean validateUSDto(USBankAccountDto dto, ConstraintValidatorContext context) {
        boolean isValid = true;
        // Required fields for US
        if (dto.getBankName() == null || dto.getBankName().trim().isEmpty()) {
            addViolation(context, "bankName", "bankName is required for US");
            isValid = false;
        }
        if (dto.getAccountNumber() == null || dto.getAccountNumber().trim().isEmpty()) {
            addViolation(context, "accountNumber", "accountNumber is required for US");
            isValid = false;
        }
        if (dto.getAccountType() == null || dto.getAccountType().trim().isEmpty()) {
            addViolation(context, "accountType", "accountType is required for US");
            isValid = false;
        }
        if (dto.getCurrency() == null || dto.getCurrency().trim().isEmpty()) {
            addViolation(context, "currency", "currency is required for US");
            isValid = false;
        }
        if (dto.getBankCountry() == null || dto.getBankCountry().trim().isEmpty()) {
            addViolation(context, "bankCountry", "bankCountry is required for US");
            isValid = false;
        }
        if (dto.getSwiftCode() == null || dto.getSwiftCode().trim().isEmpty()) {
            addViolation(context, "swiftCode", "swiftCode is required for US");
            isValid = false;
        }
        if (dto.getRoutingNumber() == null || dto.getRoutingNumber().trim().isEmpty()) {
            addViolation(context, "routingNumber", "routingNumber is required for US");
            isValid = false;
        }
        if (dto.getBeneficiaryName() == null || dto.getBeneficiaryName().trim().isEmpty()) {
            addViolation(context, "beneficiaryName", "beneficiaryName is required for US");
            isValid = false;
        }
        if (dto.getBeneficiaryAddress() == null || dto.getBeneficiaryAddress().trim().isEmpty()) {
            addViolation(context, "beneficiaryAddress", "beneficiaryAddress is required for US");
            isValid = false;
        }
//        if (dto.getPaymentMethod() == null || dto.getPaymentMethod().trim().isEmpty()) {
//            addViolation(context, "paymentMethod", "paymentMethod is required for US");
//            return false;
//        }

        // Numeric validation
        if (!isNumeric(dto.getAccountNumber())) {
            addViolation(context, "accountNumber", "accountNumber must be numeric");
            isValid = false;
        }
        if (!isNumeric(dto.getRoutingNumber())) {
            addViolation(context, "routingNumber", "routingNumber must be numeric");
            isValid = false;
        }

        return isValid;
    }

    /**
     * Checks if a string contains only digits.
     */
    private boolean isNumeric(String value) {
        return value == null || value.matches("\\d+");
    }

    /**
     * Adds a constraint violation for a specific field within bankAccountDetails.
     */
    private void addViolation(ConstraintValidatorContext context, String field, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode("bankAccountDetails").addPropertyNode(field)
                .addConstraintViolation();
    }
}