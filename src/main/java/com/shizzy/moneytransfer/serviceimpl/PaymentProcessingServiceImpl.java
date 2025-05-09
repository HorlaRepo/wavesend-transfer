package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.api.GenericResponse;
import com.shizzy.moneytransfer.dto.*;
import com.shizzy.moneytransfer.kafka.NotificationProducer;
import com.shizzy.moneytransfer.repository.TransactionReferenceRepository;
import com.shizzy.moneytransfer.repository.TransactionRepository;
import com.shizzy.moneytransfer.repository.WalletRepository;
import com.shizzy.moneytransfer.service.*;
import com.shizzy.moneytransfer.service.MoneyTransferService;
import com.shizzy.moneytransfer.service.PaymentProcessingService;
import com.shizzy.moneytransfer.service.WithdrawalService;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentProcessingServiceImpl implements PaymentProcessingService {

    private final TransactionRepository transactionRepository;
    private final WalletService walletService;
    private final TransactionReferenceService referenceService;
    private final TransactionService transactionService;
    private final TransactionReferenceRepository referenceRepository;
    private final WalletRepository walletRepository;
    private PaymentService flutterwaveService;
    private PaymentService stripeService;
    private final FlutterwaveService flutterService;
    private final TransactionServiceImpl transactionServiceImpl;
    private final KeycloakService keycloakService;
    private final NotificationProducer notificationProducer;
    private final WithdrawalService withdrawalService;
    private final MoneyTransferService moneyTransferService;


    @Autowired
    @Qualifier("flutterwaveService")
    public void setFlutterwaveService(PaymentService flutterwaveService) {
        this.flutterwaveService = flutterwaveService;
    }

    @Autowired
    @Qualifier("stripeService")
    public void setStripeService(PaymentService stripeService) {
        this.stripeService = stripeService;
    }

    @Override
    @Transactional
    public GenericResponse<WithdrawalData> withdraw(WithdrawalRequestMapper requestMapper) {

        return withdrawalService.withdraw(requestMapper);
    }

    @Override
    @Transactional
    public PaymentResponse createStripePayment(double amount, String email) throws Exception {
        return stripeService.createPayment(amount, email);
    }

    @Override
    public ResponseEntity<String> handleStripeWebhook(String payload) {
        return stripeService.handleWebhook(payload);
    }

    @Override
    public ResponseEntity<String> handleFlutterwaveWebhook(WebhookPayload payload) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'handleFlutterwaveWebhook'");
    }

    @Override
    public ApiResponse<TransactionResponseDTO> sendMoney(@NotNull CreateTransactionRequestBody requestBody) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendMoney'");
    }


}
