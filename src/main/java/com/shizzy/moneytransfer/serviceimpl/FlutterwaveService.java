package com.shizzy.moneytransfer.serviceimpl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.api.GenericResponse;
import com.shizzy.moneytransfer.dto.*;
import com.shizzy.moneytransfer.enums.TransactionOperation;
import com.shizzy.moneytransfer.enums.TransactionStatus;
import com.shizzy.moneytransfer.enums.TransactionType;
import com.shizzy.moneytransfer.kafka.NotificationProducer;
import com.shizzy.moneytransfer.model.BankAccount;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.model.TransactionReference;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.repository.TransactionReferenceRepository;
import com.shizzy.moneytransfer.repository.TransactionRepository;
import com.shizzy.moneytransfer.service.KeycloakService;
import com.shizzy.moneytransfer.service.PaymentService;
import com.shizzy.moneytransfer.service.TransactionReferenceService;
import com.shizzy.moneytransfer.service.WalletService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.web.client.HttpStatusCodeException;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlutterwaveService implements PaymentService {

    private final TransactionRepository transactionRepository;
    private final TransactionReferenceService referenceService;
    private final TransactionReferenceRepository referenceRepository;
    private final WalletService walletService;
    private NotificationProducer notificationProducer;
    private final KeycloakService keycloakService;

    @Value("${flutterwave.api.live-key}")
    private String apiKey;

    @Value("${flutterwave.api.base-url}")
    private String baseUrl;

    private WebClient webClient;
    private static final Logger logger = LoggerFactory.getLogger(FlutterwaveService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public Mono<FlutterwaveResponse> getBanks(String country) {
        return this.webClient.get()
                .uri(baseUrl + "/banks/{country}", country)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(FlutterwaveResponse.class);
    }

    @Override
    public Mono<ExchangeRateResponse> getExchangeRate(ExchangeRateRequest request) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/transfers/rates")
                        .queryParam("amount", request.getAmount())
                        .queryParam("destination_currency", request.getDestinationCurrency())
                        .queryParam("source_currency", request.getSourceCurrency())
                        .build())
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(ExchangeRateResponse.class);
    }

    @Override
    public GenericResponse<Beneficiary> addBeneficiary(AddBeneficiaryRequest beneficiary) {
        String url = baseUrl + "/beneficiaries";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<AddBeneficiaryRequest> request = new HttpEntity<>(beneficiary, headers);

        ResponseEntity<GenericResponse<Beneficiary>> response = restTemplate.exchange(url, HttpMethod.POST, request,
                new ParameterizedTypeReference<>() {
                });

        return response.getBody();
    }

    @Override
    public GenericResponse<WithdrawalData> withdraw(FlutterwaveWithdrawalRequest withdrawalRequest) {
        String url = baseUrl + "/transfers";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        try {
            // Convert the object to a JSON string first
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            // Use JsonInclude.Include.NON_NULL to exclude null fields
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            String jsonBody = mapper.writeValueAsString(withdrawalRequest);
            log.info("Withdrawal request JSON: {}", jsonBody);

            // Create HttpEntity with the JSON string
            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> rawResponse = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class);

            log.info("Flutterwave API raw response: {}", rawResponse.getBody());

            // Parse the response back to our expected type
            GenericResponse<WithdrawalData> response = mapper.readValue(
                    rawResponse.getBody(),
                    new TypeReference<GenericResponse<WithdrawalData>>() {
                    });

            return response;

        } catch (JsonProcessingException e) {
            log.error("Error serializing withdrawal request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process withdrawal request", e);
        } catch (HttpStatusCodeException e) {
            log.error("API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("API error: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Unexpected error during withdrawal: {}", e.getMessage(), e);
            throw new RuntimeException("Withdrawal process failed", e);
        }
    }

    @Override
    public GenericResponse<List<FeeData>> getFees(double amount, String currency) {
        String url = baseUrl + "/transfers/fee";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("amount", amount)
                .queryParam("currency", currency);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<GenericResponse<List<FeeData>>> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {
                });

        return response.getBody();
    }

    @Override
    public ResponseEntity<String> handleWebhook(WebhookPayload payload) {
        processTransaction(payload);
        return ResponseEntity.ok("Webhook received");
    }

    @Override
    public ResponseEntity<String> handleWebhook(String payload) {
        return null;
    }

    @Override
    public PaymentResponse createPayment(double amount, String email) throws Exception {
        throw new UnsupportedOperationException("Flutterwave does not support  deposit yet");
    }

    @Override
    public GenericResponse<String> deleteBeneficiary(Integer beneficiaryId) {
        String url = baseUrl + "/beneficiaries/" + beneficiaryId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<GenericResponse<String>> response = restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                entity,
                new ParameterizedTypeReference<>() {
                });

        return response.getBody();
    }

    @Override
    public ApiResponse<String> processRefund(RefundRequest refundRequest) {
        return null;
    }

    public FlutterwaveWithdrawalRequest buildFlutterwaveWithdrawalRequest(WithdrawalInfo withdrawalInfo,
            String reference) {
        BankAccount withdrawalAccount = withdrawalInfo.getBankAccount();
        String region = withdrawalAccount.getRegion();

        log.info("Building withdrawal request for region: {}, amount: {}, currency: {}, reference: {}",
                region, withdrawalInfo.getAmount(), withdrawalInfo.getCurrency(), reference);

        // Start with base fields that are common to all regions
        FlutterwaveWithdrawalRequest.FlutterwaveWithdrawalRequestBuilder withdrawalRequestBuilder = FlutterwaveWithdrawalRequest
                .builder()
                .amount(withdrawalInfo.getAmount())
                .currency(withdrawalInfo.getCurrency())
                .narration(withdrawalInfo.getNarration())
                .reference(reference);

        // Region-specific configuration
        switch (region.toLowerCase()) {
            case "africa":
                log.info("Processing African bank transfer - Bank code: {}, Account number: {}",
                        withdrawalAccount.getBankCode(), withdrawalAccount.getAccountNumber());

                withdrawalRequestBuilder.account_bank(withdrawalAccount.getBankCode());
                withdrawalRequestBuilder.account_number(withdrawalAccount.getAccountNumber());

                // For Mobile Money, add beneficiary name
                if ("Mobile Money".equals(withdrawalAccount.getPaymentMethod())) {
                    withdrawalRequestBuilder.beneficiary_name(withdrawalAccount.getAccountName());
                }
                break;

            case "eu":
                log.info("Processing EU bank transfer - Bank: {}, Account: {}, Swift: {}",
                        withdrawalAccount.getBankName(),
                        maskAccountNumber(withdrawalAccount.getAccountNumber()),
                        withdrawalAccount.getSwiftCode());

                withdrawalRequestBuilder.beneficiary_name(withdrawalAccount.getBeneficiaryName());
                WithdrawalMeta euMeta = WithdrawalMeta.builder()
                        .bank_name(withdrawalAccount.getBankName())
                        .account_number(withdrawalAccount.getAccountNumber())
                        .routing_number(withdrawalAccount.getRoutingNumber())
                        .swift_code(withdrawalAccount.getSwiftCode())
                        .beneficiary_name(withdrawalAccount.getBeneficiaryName())
                        .beneficiary_country(withdrawalAccount.getBeneficiaryCountry())
                        .postal_code(withdrawalAccount.getPostalCode())
                        .street_number(withdrawalAccount.getStreetNumber())
                        .street_name(withdrawalAccount.getStreetName())
                        .city(withdrawalAccount.getCity())
                        .build();
                withdrawalRequestBuilder.meta(List.of(euMeta));
                break;

            case "us":
                log.info("Processing US bank transfer - Bank: {}, Account: {}, Routing: {}",
                        withdrawalAccount.getBankName(),
                        maskAccountNumber(withdrawalAccount.getAccountNumber()),
                        withdrawalAccount.getRoutingNumber());

                withdrawalRequestBuilder.beneficiary_name(withdrawalAccount.getBeneficiaryName());
                WithdrawalMeta usMeta = WithdrawalMeta.builder()
                        .bank_name(withdrawalAccount.getBankName())
                        .account_number(withdrawalAccount.getAccountNumber())
                        .routing_number(withdrawalAccount.getRoutingNumber())
                        .swift_code(withdrawalAccount.getSwiftCode())
                        .beneficiary_name(withdrawalAccount.getBeneficiaryName())
                        .beneficiary_country(withdrawalAccount.getBeneficiaryCountry())
                        .beneficiary_address(withdrawalAccount.getBeneficiaryAddress())
                        .build();
                withdrawalRequestBuilder.meta(List.of(usMeta));
                break;

            default:
                log.error("Unsupported region: {}", region);
                throw new IllegalStateException("Unsupported region: " + region);
        }

        // Build the request object
        FlutterwaveWithdrawalRequest request = withdrawalRequestBuilder.build();
        log.debug("Built withdrawal request: {}", request);
        return request;
    }

    private void processTransaction(WebhookPayload payload) {
        if ("transfer.completed".equals(payload.getEvent())) {
            Transaction transaction = transactionRepository
                    .findTransactionByReferenceNumber(payload.getData().getReference()).get(0);
            if (payload.getData().getStatus().equals("SUCCESSFUL")) {
                if (TransactionStatus.PENDING.getValue().equals(transaction.getCurrentStatus())) {
                    updateSuccessfulTransaction(payload);
                }
            } else if (payload.getData().getStatus().equals("FAILED")) {
                updateFailedTransaction(payload);
                reverseTransaction(payload);
            }
        }
    }

    private void updateSuccessfulTransaction(WebhookPayload payload) {
        Transaction transaction = transactionRepository
                .findTransactionByReferenceNumber(payload.getData().getReference()).get(0);
        transaction.setCurrentStatus(TransactionStatus.SUCCESS.getValue());
        transaction.setNarration(payload.getData().getNarration());
        transactionRepository.save(transaction);

        TransferInfo transferInfo = getTransferInfo(transaction.getWallet().getCreatedBy());

        TransactionNotification transactionNotification = TransactionNotification.builder()
                .debitTransaction(transaction)
                .transferInfo(transferInfo)
                .operation(TransactionOperation.WITHDRAWAL)
                .build();

        notificationProducer.sendNotification("notifications", transactionNotification);

    }

    private void updateFailedTransaction(WebhookPayload payload) {
        Transaction transaction = transactionRepository
                .findTransactionByReferenceNumber(payload.getData().getReference()).get(0);
        System.out.println("The Transaction: " + transaction.getCurrentStatus());
        transaction.setCurrentStatus(TransactionStatus.FAILED.getValue());
        transaction.setNarration(payload.getData().getNarration() + " /FAILED");
        transactionRepository.save(transaction);
    }

    private void reverseTransaction(WebhookPayload payload) {
        Transaction failedTransaction = transactionRepository
                .findTransactionByReferenceNumber(payload.getData().getReference()).get(0);

        Wallet wallet = failedTransaction.getWallet();

        Transaction transaction = Transaction.builder()
                .transactionDate(LocalDateTime.now())
                .wallet(wallet)
                .narration("Refund for failed transaction")
                .amount(failedTransaction.getAmount())
                .fee(failedTransaction.getFee())
                .currentStatus(TransactionStatus.SUCCESS.getValue())
                .transactionType(TransactionType.CREDIT)
                .operation(TransactionOperation.REVERSAL)
                .description("Reversal for transaction with reference: " + failedTransaction.getReferenceNumber())
                .referenceNumber(payload.getData().getReference())
                .build();

        transactionRepository.save(transaction);

        String reference = generateAndSaveNewReference(transaction);

        transaction.setReferenceNumber(reference);
        transactionRepository.save(transaction);

        walletService.deposit(wallet, transaction.getAmount().add(BigDecimal.valueOf(transaction.getFee())));
    }

    private String generateAndSaveNewReference(Transaction transaction) {
        String reference = referenceService.generateUniqueReferenceNumber();

        TransactionReference transactionReference = TransactionReference.builder()
                .creditTransaction(transaction)
                .referenceNumber(reference)
                .build();
        referenceRepository.save(transactionReference);

        return reference;
    }

    private TransferInfo getTransferInfo(String userId) {
        UserRepresentation userRepresentation = keycloakService.getUserById(userId).getData();

        return TransferInfo.builder()
                .senderName(userRepresentation.getFirstName() + " " + userRepresentation.getLastName())
                .senderEmail(userRepresentation.getEmail())
                .senderId(userRepresentation.getId())
                .build();
    }

    // Helper method to mask account number for logging
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }

        int visibleDigits = 4;
        int length = accountNumber.length();
        String lastFour = accountNumber.substring(length - visibleDigits);

        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < length - visibleDigits; i++) {
            masked.append('*');
        }

        return masked.append(lastFour).toString();
    }

}
