package com.shizzy.moneytransfer.serviceimpl.command;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.TransactionResponse;
import com.shizzy.moneytransfer.dto.TransactionStatusDTO;
import com.shizzy.moneytransfer.events.TransactionStatusUpdateEvent;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.model.TransactionStatus;
import com.shizzy.moneytransfer.repository.TransactionRepository;
import com.shizzy.moneytransfer.repository.TransactionStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class UpdateTransactionStatusCommand implements TransactionCommand<TransactionResponse> {
    private final TransactionRepository transactionRepository;
    private final TransactionStatusRepository statusRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Integer transactionId;
    private final TransactionStatusDTO statusDTO;


    @Override
    public ApiResponse<TransactionResponse> execute() {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        String oldStatus = transaction.getCurrentStatus();
        transaction.setCurrentStatus(statusDTO.status());

        TransactionStatus status = TransactionStatus.builder()
                .status(statusDTO.status())
                .note(statusDTO.note())
                .statusDate(LocalDateTime.now())
                .transaction(transaction)
                .build();

        statusRepository.save(status);
        Transaction updatedTransaction = transactionRepository.save(transaction);

        TransactionResponse transactionResponse = TransactionResponse.builder()
                .transactionId(updatedTransaction.getTransactionId())
                .providerId(updatedTransaction.getProviderId())
                .amount(updatedTransaction.getAmount())
                .currentStatus(updatedTransaction.getCurrentStatus())
                .transactionDate(updatedTransaction.getTransactionDate().toString())
                .referenceNumber(updatedTransaction.getReferenceNumber())
                .description(updatedTransaction.getDescription())
                .narration(updatedTransaction.getNarration())
                .fee(updatedTransaction.getFee())
                .operation(updatedTransaction.getOperation())
                .transactionType(updatedTransaction.getTransactionType())
                .sessionId(updatedTransaction.getSessionId())
                .flagged(updatedTransaction.isFlagged())
                .refundStatus(updatedTransaction.getRefundStatus())
                .walletId(updatedTransaction.getWallet().getWalletId())
                .build();

        // Publish event for observers
        eventPublisher.publishEvent(new TransactionStatusUpdateEvent(
                this, updatedTransaction, oldStatus, statusDTO.status()));

        return ApiResponse.<TransactionResponse>builder()
                .success(true)
                .message("Transaction status updated successfully")
                .data(transactionResponse)
                .build();
    }
}
