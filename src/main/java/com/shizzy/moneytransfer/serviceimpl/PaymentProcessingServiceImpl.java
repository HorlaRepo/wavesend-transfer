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
//        WithdrawalInfo withdrawalInfo = requestMapper.getWithdrawalInfo();
//        String referenceNumber = referenceService.generateUniqueReferenceNumber();
//
//        FlutterwaveWithdrawalRequest withdrawalRequest = flutterService.buildFlutterwaveWithdrawalRequest(withdrawalInfo, referenceNumber);
//        ObjectMapper objectMapper = new ObjectMapper();
//        String withdrawalRequestData = "";
//        try {
//           withdrawalRequestData = objectMapper.writeValueAsString(withdrawalRequest);
//            System.out.println(withdrawalRequestData);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//
//        Wallet wallet = walletRepository.findWalletByWalletId(requestMapper.getWalletId()).orElseThrow(()-> new ResourceNotFoundException("Wallet not found"));
//        walletService.verifyWalletBalance(wallet.getBalance(), BigDecimal.valueOf(requestMapper.getAmount()));
//
//        double transactionFee = transactionService.getTransactionFee(requestMapper.getAmount()).getData().getFee();
//
//        Transaction transaction = Transaction.builder()
//                .referenceNumber(referenceNumber)
//                .transactionDate(LocalDateTime.now())
//                .amount(BigDecimal.valueOf(requestMapper.getAmount()))
//                .currentStatus(TransactionStatus.PENDING.getValue())
//                .fee(transactionFee)
//                .transactionType(TransactionType.DEBIT)
//                .wallet(wallet)
//                .narration("WaveSend - " + requestMapper.getWithdrawalInfo().getNarration())
//                .description("Withdrawal to "+ requestMapper.getWithdrawalInfo().getBankAccount().getBankName())
//                .build();
//
//        transactionRepository.save(transaction);
//
//        TransactionReference transactionReference = TransactionReference.builder()
//                .debitTransaction(transaction)
//                .referenceNumber(referenceNumber)
//                .build();
//
//        referenceRepository.save(transactionReference);
//
//        walletService.debit(wallet, BigDecimal.valueOf(requestMapper.getAmount()).add(BigDecimal.valueOf(requestMapper.getFee())));
//
//        return flutterwaveService.withdraw(withdrawalRequest);
    }

    @Override
    public ResponseEntity<String> handleFlutterwaveWebhook(WebhookPayload payload) {
        System.out.println(payload);
        return flutterwaveService.handleWebhook(payload);
    }

    @Override
    @Transactional
    public ApiResponse<TransactionResponseDTO> sendMoney(@NotNull CreateTransactionRequestBody requestBody) {
        return moneyTransferService.transfer(requestBody);

//        BigDecimal remainingAmount = requestBody.amount();
//
//        validateSenderReceiver(requestBody.senderEmail(), requestBody.receiverEmail());
//
//        TransferInfo transferInfo = fetchSenderAndReceiverId(requestBody.senderEmail(), requestBody.receiverEmail());
//
//        Wallet sendingWallet = walletService.findWalletOrThrow(transferInfo.getSenderId());
//        Wallet receivingWallet = walletService.findWalletOrThrow(transferInfo.getReceiverId());
//
//        walletService.verifyWalletBalance(sendingWallet.getBalance(), requestBody.amount());
//
//        String referenceNumber = referenceService.generateUniqueReferenceNumber();
//        String debitDescription = "Wallet to wallet Transfer to " +transferInfo.getReceiverName();
//        String creditDescription = "Wallet to wallet Transfer from " + transferInfo.getSenderName();
//
//        final Transaction debitTransaction = transactionServiceImpl.createTransaction(
//                sendingWallet,
//                requestBody,
//                TransactionType.DEBIT,
//                debitDescription,
//                referenceNumber
//        );
//
//        debitTransaction.setCurrentStatus(TransactionStatus.PENDING.getValue());
//
//        transactionRepository.save(debitTransaction);
//
//        final Transaction creditTransaction = transactionServiceImpl.createTransaction(
//                receivingWallet,
//                requestBody,
//                TransactionType.CREDIT,
//                creditDescription,
//                referenceNumber
//        );
//
//        creditTransaction.setCurrentStatus(TransactionStatus.PENDING.getValue());
//
//        transactionRepository.save(creditTransaction);
//
//        walletService.transfer(sendingWallet,receivingWallet, requestBody.amount());
//
//        TransactionReference transactionReference = TransactionReference.builder()
//                .referenceNumber(referenceNumber)
//                .debitTransaction(debitTransaction)
//                .creditTransaction(creditTransaction)
//                .build();
//
//        referenceService.saveTransactionReference(transactionReference, "");
//
//        creditTransaction.setCurrentStatus(TransactionStatus.SUCCESS.getValue());
//        debitTransaction.setCurrentStatus(TransactionStatus.SUCCESS.getValue());
//
//        transactionRepository.save(creditTransaction);
//        transactionRepository.save(debitTransaction);
//
//        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEE MMMM dd uuuu", Locale.getDefault());
//
//        TransactionResponseDTO responseDTO = new TransactionResponseDTO(
//                dtf.format(debitTransaction.getTransactionDate()),
//                debitTransaction.getAmount(),
//                debitTransaction.getCurrentStatus(),
//                debitTransaction.getTransactionType(),
//                sendingWallet.getCreatedBy(),
//                sendingWallet.getWalletId(),
//                receivingWallet.getCreatedBy(),
//                receivingWallet.getWalletId(),
//                referenceNumber
//        );
//
//        updateRefundableAmount(sendingWallet, remainingAmount);
//
//        TransactionNotification notification = TransactionNotification.builder()
//                .operation(TransactionOperation.TRANSFER)
//                .transferInfo(transferInfo)
//                .debitTransaction(debitTransaction)
//                .creditTransaction(creditTransaction)
//                .build();
//
//        notificationProducer.sendNotification("notifications",notification);
//
//        return ApiResponse.<TransactionResponseDTO>builder()
//                .success(true)
//                .message("Transfer successful")
//                .data(responseDTO)
//                .build();
    }

//    private void updateRefundableAmount(Wallet sendingWallet, BigDecimal remainingAmount) {
//        List<Transaction> senderDeposits = transactionRepository
//                .findByWalletIdAndOperationAndRefundStatusNot(sendingWallet.getId(), TransactionOperation.DEPOSIT, RefundStatus.NON_REFUNDABLE);
//
//        for (Transaction deposit: senderDeposits) {
//            BigDecimal refundableAmount = deposit.getRefundableAmount();
//            if (remainingAmount.compareTo(refundableAmount) >= 0) {
//                remainingAmount = remainingAmount.subtract(refundableAmount);
//                deposit.setRefundableAmount(BigDecimal.ZERO);
//                deposit.setRefundStatus(RefundStatus.NON_REFUNDABLE);
//            } else {
//                deposit.setRefundableAmount(refundableAmount.subtract(remainingAmount));
//                deposit.setRefundStatus(RefundStatus.PARTIALLY_REFUNDABLE);
//                remainingAmount = BigDecimal.ZERO;
//            }
//            transactionRepository.save(deposit);
//
//            if (remainingAmount.compareTo(BigDecimal.ZERO) == 0) {
//                break;
//            }
//        }
//    }

    @Override
    @Transactional
    public PaymentResponse createStripePayment(double amount, String email) throws Exception {
        return stripeService.createPayment(amount, email);
    }

    @Override
    public ResponseEntity<String> handleStripeWebhook(String payload) {
        return stripeService.handleWebhook(payload);
    }

//    private void validateSenderReceiver(@NotNull String senderEmail, @NotNull String receiverEmail) {
//        if (senderEmail.equals(receiverEmail)) {
//            throw new IllegalArgumentException("You cannot send money to yourself.");
//        }
//    }

//    private TransferInfo fetchSenderAndReceiverId(String senderEmail, String receiverEmail){
//        UserRepresentation sender = keycloakService.existsUserByEmail(senderEmail).getData();
//        UserRepresentation receiver = keycloakService.existsUserByEmail(receiverEmail).getData();
//
//        return TransferInfo.builder()
//                .senderId(sender.getId())
//                .receiverId(receiver.getId())
//                .senderName(sender.getFirstName() +" " + sender.getLastName())
//                .receiverName(receiver.getFirstName() +" " + receiver.getLastName())
//                .senderEmail(sender.getEmail())
//                .receiverEmail(receiver.getEmail())
//                .build();
//    }
}
