package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.enums.RefundStatus;
import com.shizzy.moneytransfer.service.WalletService;
import com.shizzy.moneytransfer.serviceimpl.command.UpdateTransactionStatusCommand;
import com.shizzy.moneytransfer.util.CacheNames;
import com.shizzy.moneytransfer.enums.TransactionOperation;
import com.shizzy.moneytransfer.service.TransactionFeeService;
import com.shizzy.moneytransfer.service.TransactionFilterService;
import com.shizzy.moneytransfer.service.TransactionService;
import com.shizzy.moneytransfer.dto.*;
import com.shizzy.moneytransfer.enums.TransactionType;
import com.shizzy.moneytransfer.exception.*;
import com.shizzy.moneytransfer.model.*;
import com.shizzy.moneytransfer.repository.*;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.shizzy.moneytransfer.util.CacheNames.*;
import static com.shizzy.moneytransfer.util.TransactionSpecification.buildSpecification;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionStatusRepository statusRepository;
    private final WalletRepository walletRepository;
    private final TransactionFeeService transactionFeeService;
    private final TransactionFilterService transactionFilterService;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionReferenceRepository referenceRepository;
    private final WalletService walletService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Cacheable(value = TRANSACTIONS, key = "'allTransactions:' + #pageNumber + ':' + #pageSize", unless = "#result.data == null")
    public ApiResponse<PagedTransactionResponse> getAllTransactions(int pageNumber, int pageSize) {
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "transactionId"));
        Page<Transaction> transactions = transactionRepository.findAll(pageRequest);

        if (transactions.isEmpty()) {
            throw new ResourceNotFoundException("No transactions found");
        }

        List<TransactionResponse> transactionResponses = transactions.getContent().stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());

        PagedTransactionResponse pagedResponse = PagedTransactionResponse.builder()
                .content(transactionResponses)
                .pageNumber(transactions.getNumber())
                .pageSize(transactions.getSize())
                .totalElements(transactions.getTotalElements())
                .totalPages(transactions.getTotalPages())
                .first(transactions.isFirst())
                .last(transactions.isLast())
                .build();

        return ApiResponse.<PagedTransactionResponse>builder()
                .success(true)
                .message(transactions.getTotalElements() + " transactions found")
                .data(pagedResponse)
                .build();
    }

    @Override
    @Cacheable(value = TRANSACTIONS, key = "'refNum:' + #referenceNumber", unless = "#result.data.isEmpty()")
    public ApiResponse<List<TransactionResponse>> getTransactionByReferenceNumber(String referenceNumber) {
        List<Transaction> transactions = transactionRepository.findTransactionByReferenceNumber(referenceNumber);

        if (transactions.isEmpty()) {
            throw new ResourceNotFoundException("No transactions found");
        }

        List<TransactionResponse> transactionResponses = transactions.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());

        return ApiResponse.<List<TransactionResponse>>builder()
                .success(true)
                .message(transactionResponses.size() + " transactions found")
                .data(transactionResponses)
                .build();
    }

    @Override
    @Cacheable(value = SINGLE_TRANSACTION, key = "'tx:' + #transactionId", unless = "#result.data == null")
    public ApiResponse<TransactionResponse> getTransactionById(Integer transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("transaction not found"));

        TransactionResponse transactionResponse = mapToTransactionResponse(transaction);

        return ApiResponse.<TransactionResponse>builder()
                .success(true)
                .message("Transaction found")
                .data(transactionResponse)
                .build();
    }

    @Override
    @Cacheable(value = SEARCH_RESULT, key = "'search:' + #searchQuery + ':' + #searchFilter + ':' + #sortOrder + ':' + #page + ':' + #size", unless = "#result.data.content.isEmpty()")
    public ApiResponse<PagedTransactionResponse> searchTransactions(String searchQuery, String sortOrder,
            String searchFilter, int page, int size) {
        Specification<Transaction> specification = buildSpecification(searchQuery, searchFilter);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(sortOrder), "transactionDate"));
        Page<Transaction> transactions = transactionRepository.findAll(specification, pageable);

        List<TransactionResponse> transactionResponses = transactions.getContent().stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());

        PagedTransactionResponse pagedResponse = PagedTransactionResponse.builder()
                .content(transactionResponses)
                .pageNumber(transactions.getNumber())
                .pageSize(transactions.getSize())
                .totalElements(transactions.getTotalElements())
                .totalPages(transactions.getTotalPages())
                .first(transactions.isFirst())
                .last(transactions.isLast())
                .build();

        return ApiResponse.<PagedTransactionResponse>builder()
                .success(true)
                .message(transactions.getTotalElements() + " transactions found")
                .data(pagedResponse)
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = { TRANSACTIONS, SINGLE_TRANSACTION, ALL_USER_TRANSACTION }, allEntries = true)
    public ApiResponse<TransactionResponse> updateTransactionStatus(String referenceNumber,
            UpdateTransactionRequest request) {
        Transaction transaction = transactionRepository.findTransactionByReferenceNumber(referenceNumber).get(0);
        String oldStatus = transaction.getCurrentStatus();
        transaction.setCurrentStatus(request.getStatus());
        Transaction updatedTransaction = transactionRepository.save(transaction);

        // Create transaction status record
        TransactionStatus status = TransactionStatus.builder()
                .status(request.getStatus())
                .statusDate(LocalDateTime.now())
                .transaction(transaction)
                .build();
        statusRepository.save(status);

        return ApiResponse.<TransactionResponse>builder()
                .success(true)
                .message("Transaction updated successfully")
                .data(mapToTransactionResponse(updatedTransaction))
                .build();
    }

    @Override
    @Cacheable(value = TRANSACTIONS, key = "'status:' + #referenceNumber")
    public String getTransactionStatus(String referenceNumber) {
        Transaction transaction = transactionRepository.findTransactionByReferenceNumber(referenceNumber).get(0);
        return transaction.getCurrentStatus();
    }

    @Override
    @Cacheable(value = SINGLE_TRANSACTION, key = "'refLookup:' + #referenceNumber")
    public Transaction findByReferenceNumber(String referenceNumber) {
        List<Transaction> transactions = transactionRepository.findTransactionByReferenceNumber(referenceNumber);
        return transactions.isEmpty() ? null : transactions.get(0);
    }

    @Override
    @Cacheable(value = SINGLE_TRANSACTION, key = "'byId:' + #id")
    public Transaction findById(Integer id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
    }

    @Override
    @Transactional
    @CacheEvict(value = { TRANSACTIONS, SINGLE_TRANSACTION }, key = "'tx:' + #transaction.transactionId")
    public void completeDeposit(Transaction transaction, String sessionId, String providerId, BigDecimal amount,
            RefundStatus refundStatus) {
        transaction.setCurrentStatus(com.shizzy.moneytransfer.enums.TransactionStatus.SUCCESS.getValue());
        transaction.setSessionId(sessionId);
        transaction.setRefundableAmount(amount);
        transaction.setRefundStatus(refundStatus);
        transaction.setProviderId(providerId);
        transactionRepository.save(transaction);

        walletService.deposit(transaction.getWallet(), amount);

        redisTemplate.delete(WALLETS + "::walletId:" + transaction.getWallet().getWalletId());

    }

    @Override
    @Transactional
    @CachePut(value = SINGLE_TRANSACTION, key = "'tx:' + #result.transactionId")
    public Transaction createReversalTransaction(Wallet wallet, BigDecimal amount, String description,
            TransactionOperation operation) {
        Transaction transaction = Transaction.builder()
                .amount(amount)
                .currentStatus(com.shizzy.moneytransfer.enums.TransactionStatus.SUCCESS.getValue())
                .transactionDate(LocalDateTime.now())
                .description(description)
                .operation(operation)
                .transactionType(TransactionType.CREDIT)
                .wallet(wallet)
                .build();

        return transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public Transaction createRefundTransaction(Transaction originalTransaction, BigDecimal amount,
            String referenceNumber) {
        Transaction refundTransaction = Transaction.builder()
                .amount(amount)
                .currentStatus(com.shizzy.moneytransfer.enums.TransactionStatus.PENDING.getValue())
                .transactionDate(LocalDateTime.now())
                .description("Refund for transaction " + originalTransaction.getReferenceNumber())
                .operation(TransactionOperation.REFUND)
                .transactionType(TransactionType.DEBIT)
                .wallet(originalTransaction.getWallet())
                .referenceNumber(referenceNumber)
                .build();

        Transaction saved = transactionRepository.save(refundTransaction);

        TransactionReference reference = TransactionReference.builder()
                .referenceNumber(referenceNumber)
                .debitTransaction(originalTransaction)
                .build();
        referenceRepository.save(reference);

        return saved;
    }

    @Override
    public void updateTransactionStatus(Integer transactionId, com.shizzy.moneytransfer.enums.TransactionStatus status,
            String narration) {
        Transaction transaction = findById(transactionId);
        transaction.setCurrentStatus(status.getValue());
        if (narration != null) {
            transaction.setNarration(narration);
        }
        transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public void restoreRefundableAmount(Integer depositId, BigDecimal amount) {
        Transaction transaction = findById(depositId);
        BigDecimal newRefundableAmount = transaction.getRefundableAmount().add(amount);
        transaction.setRefundableAmount(newRefundableAmount);

        if (newRefundableAmount.compareTo(transaction.getAmount()) == 0) {
            transaction.setRefundStatus(RefundStatus.FULLY_REFUNDABLE);
        } else {
            transaction.setRefundStatus(RefundStatus.PARTIALLY_REFUNDABLE);
        }

        transactionRepository.save(transaction);
    }

    @Override
    @Cacheable(value = TRANSACTIONS, key = "'fee:' + #amount", unless = "#amount <= 0")
    public ApiResponse<TransactionFee> getTransactionFee(double amount) {
        return ApiResponse.<TransactionFee>builder()
                .success(true)
                .message("Transaction fee calculated successfully")
                .data(transactionFeeService.calculateFee(amount))
                .build();
    }

    @Override
    @Cacheable(value = ALL_USER_TRANSACTION, key = "'filter:' + #walletId + ':' + #filter + ':' + #startDate + ':' + #endDate + ':' + #pageNumber + ':' + #pageSize", unless = "#result.data == null")
    public ApiResponse<PagedTransactionResponse> getTransactionsByFilter(
            Long walletId,
            String filter,
            String startDate,
            String endDate,
            int pageNumber,
            int pageSize) {

        Optional<LocalDate> startingDate = Optional.ofNullable(startDate).map(LocalDate::parse);
        Optional<LocalDate> endingDate = Optional.ofNullable(endDate).map(LocalDate::parse);

        ApiResponse<Page<Transaction>> response = transactionFilterService.getTransactionByFilter(
                walletId, filter, startingDate, endingDate, pageNumber, pageSize);

        Page<Transaction> transactions = response.getData();

        List<TransactionResponse> transactionResponses = transactions.getContent().stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());

        PagedTransactionResponse pagedResponse = PagedTransactionResponse.builder()
                .content(transactionResponses)
                .pageNumber(transactions.getNumber())
                .pageSize(transactions.getSize())
                .totalElements(transactions.getTotalElements())
                .totalPages(transactions.getTotalPages())
                .first(transactions.isFirst())
                .last(transactions.isLast())
                .build();

        return ApiResponse.<PagedTransactionResponse>builder()
                .success(true)
                .message(response.getMessage())
                .data(pagedResponse)
                .build();
    }

    @Override
    public Transaction createTransaction(Wallet wallet, CreateTransactionRequestBody requestBody,
            TransactionType transactionType, TransactionOperation operation, String description,
            String referenceNumber) {
        return Transaction.builder()
                .wallet(wallet)
                .amount(requestBody.amount())
                .transactionType(transactionType)
                .narration(requestBody.narration())
                .description(description)
                .currentStatus(com.shizzy.moneytransfer.enums.TransactionStatus.PENDING.getValue())
                .referenceNumber(referenceNumber)
                .operation(TransactionOperation.TRANSFER)
                .transactionDate(LocalDateTime.now())
                .build();
    }

    @Override
    @CacheEvict(value = { TRANSACTIONS, SINGLE_TRANSACTION, ALL_USER_TRANSACTION }, key = "'tx:' + #id")
    public ApiResponse<TransactionResponse> updateTransaction(Integer id, TransactionStatusDTO statusDTO) {
        UpdateTransactionStatusCommand command = new UpdateTransactionStatusCommand(
                transactionRepository,
                statusRepository,
                eventPublisher,
                id,
                statusDTO);

        return command.execute();
    }

    @Override
    @Cacheable(value = CacheNames.ALL_USER_TRANSACTION, key = "'wallet:' + #walletId + ':page:' + #page + ':size:' + #size")
    public ApiResponse<PagedTransactionResponse> getTransactionsByWallet(String walletId, int page, int size) {
        log.debug("Fetching transactions from database for wallet ID: {}", walletId);

        Wallet wallet = walletRepository.findWalletByWalletId(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionId"));
        Page<Transaction> transactions = transactionRepository.findTransactionsByWallet(wallet, pageable);
        log.info("Fetched {} transactions for wallet ID: {}", transactions.getTotalElements(), walletId);

        List<TransactionResponse> transactionResponses = transactions.getContent().stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());

        // ApiResponse<Page<Transaction>> response = new ApiResponse<>();
        // response.setSuccess(true);
        // response.setMessage(transactions.getTotalElements() + " transactions found");
        // response.setData(transactions);

        PagedTransactionResponse pagedResponse = PagedTransactionResponse.builder()
                .content(transactionResponses)
                .pageNumber(transactions.getNumber())
                .pageSize(transactions.getSize())
                .totalElements(transactions.getTotalElements())
                .totalPages(transactions.getTotalPages())
                .first(transactions.isFirst())
                .last(transactions.isLast())
                .build();

        return ApiResponse.<PagedTransactionResponse>builder()
                .success(true)
                .message(transactions.getTotalElements() + " transactions found")
                .data(pagedResponse)
                .build();
    }

    @Override
    @Cacheable(value = ALL_USER_TRANSACTION, key = "'byDate:' + #request.walletId + ':' + #request.startDate + ':' + #request.endDate + ':' + #request.page + ':' + #request.size", unless = "#result.data == null")
    public ApiResponse<PagedTransactionResponse> getUserTransactionsByDate(TransactionsByDateRequest request) {
        Wallet wallet = walletRepository.findWalletByWalletId(request.getWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        LocalDateTime startDate = LocalDate.parse(request.getStartDate()).atStartOfDay();
        LocalDateTime endDate = LocalDate.parse(request.getEndDate()).atStartOfDay();

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(),
                Sort.by(Sort.Direction.DESC, "transactionId"));

        Page<Transaction> transactions = transactionRepository.findTransactionsByWalletIdAndDateRange(wallet, startDate,
                endDate, pageable);

        List<TransactionResponse> transactionResponses = transactions.getContent().stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());

        PagedTransactionResponse pagedResponse = PagedTransactionResponse.builder()
                .content(transactionResponses)
                .pageNumber(transactions.getNumber())
                .pageSize(transactions.getSize())
                .totalElements(transactions.getTotalElements())
                .totalPages(transactions.getTotalPages())
                .first(transactions.isFirst())
                .last(transactions.isLast())
                .build();

        return ApiResponse.<PagedTransactionResponse>builder()
                .success(true)
                .message(transactions.getTotalElements() + " transactions found")
                .data(pagedResponse)
                .build();
    }

    /**
     * Maps a Transaction entity to a TransactionResponse DTO
     */
    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .providerId(transaction.getProviderId())
                .amount(transaction.getAmount())
                .currentStatus(transaction.getCurrentStatus())
                .transactionDate(
                        transaction.getTransactionDate() != null ? transaction.getTransactionDate().toString() : null)
                .referenceNumber(transaction.getReferenceNumber())
                .description(transaction.getDescription())
                .narration(transaction.getNarration())
                .fee(transaction.getFee())
                .operation(transaction.getOperation())
                .transactionType(transaction.getTransactionType())
                .sessionId(transaction.getSessionId())
                .flagged(transaction.isFlagged())
                .refundStatus(transaction.getRefundStatus())
                .walletId(transaction.getWallet() != null ? transaction.getWallet().getWalletId() : null)
                .build();
    }

}
