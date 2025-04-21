package com.shizzy.moneytransfer.serviceimpl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.ScheduledTransferInitiationResponse;
import com.shizzy.moneytransfer.dto.ScheduledTransferNotification;
import com.shizzy.moneytransfer.dto.ScheduledTransferRequestDTO;
import com.shizzy.moneytransfer.dto.ScheduledTransferResponseDTO;
import com.shizzy.moneytransfer.dto.ScheduledTransferVerificationRequest;
import com.shizzy.moneytransfer.enums.RecurrenceType;
import com.shizzy.moneytransfer.enums.ScheduleStatus;
import com.shizzy.moneytransfer.exception.InvalidOtpException;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.kafka.NotificationProducer;
import com.shizzy.moneytransfer.model.ScheduledTransfer;
import com.shizzy.moneytransfer.repository.ScheduledTransferRepository;
import com.shizzy.moneytransfer.service.KeycloakService;
import com.shizzy.moneytransfer.service.MoneyTransferService;
import com.shizzy.moneytransfer.service.OtpService;
import com.shizzy.moneytransfer.service.ScheduledTransferService;
import com.shizzy.moneytransfer.util.CacheNames;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTransferServiceImpl implements ScheduledTransferService {

    private final ScheduledTransferRepository scheduledTransferRepository;
    private final MoneyTransferService moneyTransferService;
    private final KeycloakService keycloakService;
    private final NotificationProducer notificationProducer;

    private final OtpService otpService;
    private final CacheManager cacheManager;

    private static final String SCHEDULE_TRANSFER_OPERATION = "Schedule Transfer";
    private static final String PENDING_SCHEDULED_TRANSFERS_CACHE = "pendingScheduledTransfersCache";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private static final int BATCH_SIZE = 100;

    @Override
    @Transactional
    @CacheEvict(value = { CacheNames.USER_SCHEDULED_TRANSFERS }, key = "#userId")
    public ApiResponse<ScheduledTransferResponseDTO> scheduleTransfer(ScheduledTransferRequestDTO request,
            String userId) {
        // Validate users exist
        keycloakService.existsUserByEmail(request.senderEmail());
        keycloakService.existsUserByEmail(request.receiverEmail());

        // Validate schedule time
        if (request.scheduledDateTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Scheduled time must be in the future");
        }

        // Validate recurrence parameters
        validateRecurrenceParams(request);

        // Create and save scheduled transfer
        ScheduledTransfer scheduledTransfer = ScheduledTransfer.builder()
                .senderEmail(request.senderEmail())
                .receiverEmail(request.receiverEmail())
                .createdBy(userId)
                .amount(request.amount())
                .description(request.description())
                .scheduledDateTime(request.scheduledDateTime())
                .status(ScheduleStatus.PENDING)
                .recurrenceType(request.recurrenceType() != null ? request.recurrenceType() : RecurrenceType.NONE)
                .recurrenceEndDate(request.recurrenceEndDate())
                .totalOccurrences(request.totalOccurrences())
                .currentOccurrence(1) // First occurrence
                .processed(false) // Mark as not processed yet
                .retryCount(0) // Initialize retry count
                .build();

        scheduledTransferRepository.save(scheduledTransfer);

        // For recurring transfers, we need to update the parentTransferId to be its own
        // ID
        if (scheduledTransfer.getRecurrenceType() != RecurrenceType.NONE) {
            scheduledTransfer.setParentTransferId(scheduledTransfer.getId());
            scheduledTransferRepository.save(scheduledTransfer);
        }

        // Send notification about scheduled transfer
        sendScheduleNotification(scheduledTransfer, "TRANSFER_SCHEDULED");

        return ApiResponse.<ScheduledTransferResponseDTO>builder()
                .success(true)
                .message("Transfer scheduled successfully")
                .data(mapToResponseDTO(scheduledTransfer))
                .build();
    }

    @Override
    @Cacheable(value = CacheNames.USER_SCHEDULED_TRANSFERS, key = "#userEmail + ':page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
    public ApiResponse<Page<ScheduledTransferResponseDTO>> getUserScheduledTransfers(String userId, Pageable pageable) {
        Page<ScheduledTransfer> transfersPage = scheduledTransferRepository
                .findByCreatedByOrderByScheduledDateTimeDesc(userId, pageable);

        Page<ScheduledTransferResponseDTO> responseDTOsPage = transfersPage.map(this::mapToResponseDTO);

        return ApiResponse.<Page<ScheduledTransferResponseDTO>>builder()
                .success(true)
                .message("Scheduled transfers retrieved successfully")
                .data(responseDTOsPage)
                .build();
    }

    @Override
    @Cacheable(value = CacheNames.USER_SCHEDULED_TRANSFERS, key = "#userEmail")
    public ApiResponse<List<ScheduledTransferResponseDTO>> getUserScheduledTransfers(String userEmail) {
        // For backward compatibility, call paginated version with a large page size
        Pageable pageable = PageRequest.of(0, 1000,
                org.springframework.data.domain.Sort.by("scheduledDateTime").descending());
        Page<ScheduledTransferResponseDTO> page = getUserScheduledTransfers(userEmail, pageable).getData();
        return ApiResponse.<List<ScheduledTransferResponseDTO>>builder()
                .success(true)
                .message("Scheduled transfers retrieved successfully")
                .data(page.getContent())
                .build();
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheNames.USER_SCHEDULED_TRANSFERS, key = "#userEmail"),
        @CacheEvict(value = CacheNames.SINGLE_SCHEDULED_TRANSFER, key = "#transferId"),
        @CacheEvict(value = CacheNames.RECURRING_SERIES, key = "#result.data.parentTransferId", condition = "#result != null && #result.data != null && #result.data.parentTransferId != null")
    })
    public ApiResponse<ScheduledTransferResponseDTO> cancelScheduledTransfer(Long transferId, String userId) {

        ScheduledTransfer transfer = scheduledTransferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Scheduled transfer not found"));

        // Check if user owns this transfer
        if (!transfer.getCreatedBy().equals(userId)) {
            throw new IllegalArgumentException("You are not authorized to cancel this transfer");
        }

        // Check if transfer is still pending
        if (transfer.getStatus() != ScheduleStatus.PENDING) {
            throw new IllegalArgumentException("Only pending transfers can be cancelled");
        }

        // Cancel the transfer
        transfer.setStatus(ScheduleStatus.CANCELLED);
        transfer.setProcessed(true); // Mark as processed since it's cancelled
        transfer.setProcessedDateTime(LocalDateTime.now());
        scheduledTransferRepository.save(transfer);

        // Send notification about cancellation
        sendScheduleNotification(transfer, "TRANSFER_CANCELLED");

        return ApiResponse.<ScheduledTransferResponseDTO>builder()
                .success(true)
                .message("Scheduled transfer cancelled successfully")
                .data(mapToResponseDTO(transfer))
                .build();
    }

    // The old @Scheduled method is removed as we now use Kafka-based processing

    @Override
    @Transactional
    @Cacheable(value = CacheNames.RECURRING_SERIES, key = "#parentId")
    public ApiResponse<List<ScheduledTransferResponseDTO>> getRecurringTransferSeries(Long parentId, String userEmail) {
        ScheduledTransfer parent = scheduledTransferRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Recurring transfer series not found"));

        // Check if user owns this series
        if (!parent.getSenderEmail().equals(userEmail)) {
            throw new IllegalArgumentException("You are not authorized to access this transfer series");
        }

        // Get all transfers in this series
        List<ScheduledTransfer> series = scheduledTransferRepository.findByParentTransferId(parentId);

        List<ScheduledTransferResponseDTO> responseDTOs = series.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());

        return ApiResponse.<List<ScheduledTransferResponseDTO>>builder()
                .success(true)
                .message("Recurring transfer series retrieved successfully")
                .data(responseDTOs)
                .build();
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheNames.USER_SCHEDULED_TRANSFERS, key = "#userEmail"),
        @CacheEvict(value = CacheNames.RECURRING_SERIES, key = "#parentId"),
        @CacheEvict(value = CacheNames.SINGLE_SCHEDULED_TRANSFER, allEntries = true)
    })
    public ApiResponse<ScheduledTransferResponseDTO> cancelRecurringSeries(Long parentId, String userEmail) {
        // Find the parent transfer
        ScheduledTransfer parent = scheduledTransferRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Recurring transfer series not found"));

        // Check if user owns this series
        if (!parent.getSenderEmail().equals(userEmail)) {
            throw new IllegalArgumentException("You are not authorized to cancel this transfer series");
        }

        // Find all pending transfers in this series
        List<ScheduledTransfer> pendingSeries = scheduledTransferRepository
                .findByParentTransferIdAndStatus(parentId, ScheduleStatus.PENDING);

        // Cancel all pending transfers
        pendingSeries.forEach(transfer -> {
            transfer.setStatus(ScheduleStatus.CANCELLED);
            transfer.setProcessed(true); // Mark as processed
            transfer.setProcessedDateTime(LocalDateTime.now());
            scheduledTransferRepository.save(transfer);
            sendScheduleNotification(transfer, "TRANSFER_CANCELLED");
        });

        return ApiResponse.<ScheduledTransferResponseDTO>builder()
                .success(true)
                .message("Recurring transfer series cancelled successfully")
                .data(mapToResponseDTO(parent))
                .build();
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheNames.USER_SCHEDULED_TRANSFERS, key = "#userEmail"),
        @CacheEvict(value = CacheNames.SINGLE_SCHEDULED_TRANSFER, key = "#transferId"),
        @CacheEvict(value = CacheNames.RECURRING_SERIES, key = "#result.data.parentTransferId", condition = "#result != null && #result.data != null && #result.data.parentTransferId != null")
    })
    public ApiResponse<ScheduledTransferResponseDTO> updateRecurringTransfer(Long transferId,
            ScheduledTransferRequestDTO request, String userEmail) {

        ScheduledTransfer transfer = scheduledTransferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Scheduled transfer not found"));

        // Check if user owns this transfer
        if (!transfer.getSenderEmail().equals(userEmail)) {
            throw new IllegalArgumentException("You are not authorized to update this transfer");
        }

        // Check if transfer is still pending
        if (transfer.getStatus() != ScheduleStatus.PENDING) {
            throw new IllegalArgumentException("Only pending transfers can be updated");
        }

        // Validate recurrence parameters if any recurrence fields are provided
        if (request.recurrenceType() != null || request.recurrenceEndDate() != null
                || request.totalOccurrences() != null) {
            validateRecurrenceParams(request);
        }

        // Update fields
        transfer.setAmount(request.amount() != null ? request.amount() : transfer.getAmount());
        transfer.setReceiverEmail(
                request.receiverEmail() != null && !request.receiverEmail().isEmpty() ? request.receiverEmail()
                        : transfer.getReceiverEmail());
        transfer.setDescription(
                request.description() != null && !request.description().isEmpty() ? request.description()
                        : transfer.getDescription());
        transfer.setScheduledDateTime(
                request.scheduledDateTime() != null ? request.scheduledDateTime() : transfer.getScheduledDateTime());
        transfer.setRecurrenceType(
                request.recurrenceType() != null ? request.recurrenceType() : transfer.getRecurrenceType());
        transfer.setRecurrenceEndDate(
                request.recurrenceEndDate() != null ? request.recurrenceEndDate() : transfer.getRecurrenceEndDate());
        transfer.setTotalOccurrences(
                request.totalOccurrences() != null ? request.totalOccurrences() : transfer.getTotalOccurrences());

        scheduledTransferRepository.save(transfer);

        return ApiResponse.<ScheduledTransferResponseDTO>builder()
                .success(true)
                .message("Transfer updated successfully")
                .data(mapToResponseDTO(transfer))
                .build();
    }

    // Make this method public so it can be called by the Kafka consumer
    @Transactional
    public void scheduleNextOccurrenceIfNeeded(ScheduledTransfer currentTransfer) {
        // Check if we should continue recurring
        if (shouldCreateNextOccurrence(currentTransfer)) {
            // Create next transfer occurrence
            ScheduledTransfer nextTransfer = createNextOccurrence(currentTransfer);
            scheduledTransferRepository.save(nextTransfer);

            log.info("Scheduled next recurring transfer with ID: {} for series: {}",
                    nextTransfer.getId(), nextTransfer.getParentTransferId());
        }
    }

    private boolean shouldCreateNextOccurrence(ScheduledTransfer transfer) {
        // If it's a one-time transfer, no next occurrence
        if (transfer.getRecurrenceType() == RecurrenceType.NONE) {
            return false;
        }

        // If we've reached the total occurrences limit
        if (transfer.getTotalOccurrences() != null &&
                transfer.getCurrentOccurrence() >= transfer.getTotalOccurrences()) {
            return false;
        }

        // If we've passed the end date
        if (transfer.getRecurrenceEndDate() != null &&
                LocalDateTime.now().isAfter(transfer.getRecurrenceEndDate())) {
            return false;
        }

        // Otherwise, we should create the next occurrence
        return true;
    }

    private ScheduledTransfer createNextOccurrence(ScheduledTransfer current) {
        // Calculate next occurrence date based on recurrence type
        LocalDateTime nextDateTime = calculateNextOccurrenceDate(current);

        // Create new transfer
        ScheduledTransfer next = ScheduledTransfer.builder()
                .senderEmail(current.getSenderEmail())
                .receiverEmail(current.getReceiverEmail())
                .amount(current.getAmount())
                .description(current.getDescription())
                .scheduledDateTime(nextDateTime)
                .status(ScheduleStatus.PENDING)
                .recurrenceType(current.getRecurrenceType())
                .recurrenceEndDate(current.getRecurrenceEndDate())
                .totalOccurrences(current.getTotalOccurrences())
                .currentOccurrence(current.getCurrentOccurrence() + 1)
                .parentTransferId(current.getParentTransferId())
                .processed(false) // Not processed yet
                .retryCount(0) // Initialize retry count
                .build();

        return next;
    }

    private LocalDateTime calculateNextOccurrenceDate(ScheduledTransfer current) {
        return switch (current.getRecurrenceType()) {
            case DAILY -> current.getScheduledDateTime().plusDays(1);
            case WEEKLY -> current.getScheduledDateTime().plusWeeks(1);
            case MONTHLY -> current.getScheduledDateTime().plusMonths(1);
            default -> current.getScheduledDateTime(); // Should never happen
        };
    }

    private void validateRecurrenceParams(ScheduledTransferRequestDTO request) {
        if (request.recurrenceType() == null || request.recurrenceType() == RecurrenceType.NONE) {
            return; // No validation needed for one-time transfers
        }

        // For recurring transfers, ensure we have either an end date or total
        // occurrences
        if (request.recurrenceEndDate() == null && request.totalOccurrences() == null) {
            throw new IllegalArgumentException("Recurring transfers must have either an end date or total occurrences");
        }

        // If end date is provided, ensure it's in the future
        if (request.recurrenceEndDate() != null && request.recurrenceEndDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Recurrence end date must be in the future");
        }

        // If total occurrences is provided, ensure it's positive
        if (request.totalOccurrences() != null && request.totalOccurrences() <= 0) {
            throw new IllegalArgumentException("Total occurrences must be positive");
        }
    }

    private ScheduledTransferResponseDTO mapToResponseDTO(ScheduledTransfer transfer) {
        return new ScheduledTransferResponseDTO(
                transfer.getId(),
                transfer.getSenderEmail(),
                transfer.getReceiverEmail(),
                transfer.getAmount(),
                transfer.getScheduledDateTime(),
                transfer.getDescription(),
                transfer.getStatus(),
                transfer.getCreatedAt(),
                transfer.getRecurrenceType(),
                transfer.getRecurrenceEndDate(),
                transfer.getTotalOccurrences(),
                transfer.getCurrentOccurrence(),
                transfer.getParentTransferId(),
                transfer.getRecurrenceType() != RecurrenceType.NONE);
    }

    private void sendScheduleNotification(ScheduledTransfer transfer, String eventType) {
        // Create notification object
        ScheduledTransferNotification notification = ScheduledTransferNotification.builder()
                .scheduledTransferId(transfer.getId())
                .senderEmail(transfer.getSenderEmail())
                .receiverEmail(transfer.getReceiverEmail())
                .amount(transfer.getAmount().doubleValue())
                .scheduledDateTime(transfer.getScheduledDateTime())
                .status(transfer.getStatus())
                .eventType(eventType)
                .timestamp(LocalDateTime.now())
                .build();

        // Send to Kafka topic
        notificationProducer.sendNotification("scheduled-transfer-notifications", notification);
        log.info("Sent {} notification for scheduled transfer ID: {}", eventType, transfer.getId());
    }

    @Override
    public void processScheduledTransfers() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'processScheduledTransfers'");
    }

    @Override
    public ApiResponse<ScheduledTransferInitiationResponse> initiateScheduledTransfer(
            ScheduledTransferRequestDTO request, String userId) {
        // Validate request and user
        ApiResponse<UserRepresentation> userResponse = keycloakService.getUserById(userId);
        if (!userResponse.isSuccess() || userResponse.getData() == null) {
            throw new ResourceNotFoundException("User not found");
        }
        var user = userResponse.getData();

        // Ensure it's the user's own transfer
        if (!user.getEmail().equals(request.senderEmail())) {
            throw new IllegalArgumentException("You can only schedule transfers from your own account");
        }

        // Generate unique token
        String scheduledTransferToken = UUID.randomUUID().toString();

        // Store pending scheduled transfer
        Cache pendingTransfersCache = cacheManager.getCache(PENDING_SCHEDULED_TRANSFERS_CACHE);
        if (pendingTransfersCache == null) {
            throw new RuntimeException("Scheduled transfer cache not configured");
        }

        // Store with user ID for security
        pendingTransfersCache.put(
                scheduledTransferToken,
                new PendingScheduledTransfer(request, userId));

        // Get recipient name (if known)
        String recipientName = getReceiverName(request.receiverEmail());

        // Prepare details for OTP email
        Map<String, Object> operationDetails = new HashMap<>();
        operationDetails.put("amount", request.amount().toString());
        operationDetails.put("recipient", recipientName);
        operationDetails.put("recipient_email", request.receiverEmail());
        operationDetails.put("scheduled_date", request.scheduledDateTime().format(DATE_FORMATTER));

        if (request.recurrenceType() != null && request.recurrenceType() != RecurrenceType.NONE) {
            operationDetails.put("is_recurring", true);
            operationDetails.put("recurrence_type", request.recurrenceType().toString());
        }

        log.debug("About to send OTP for scheduled transfer to {} with operation key {}",
                user.getEmail(), SCHEDULE_TRANSFER_OPERATION);

        // Send OTP
        otpService.sendOtp(
                user.getEmail(),
                user.getFirstName(),
                SCHEDULE_TRANSFER_OPERATION,
                operationDetails);

        return ApiResponse.<ScheduledTransferInitiationResponse>builder()
                .success(true)
                .message("Schedule transfer initiated. Please check your email for verification code.")
                .data(new ScheduledTransferInitiationResponse(scheduledTransferToken))
                .build();
    }

    @Override
    public ApiResponse<ScheduledTransferResponseDTO> verifyAndScheduleTransfer(
            ScheduledTransferVerificationRequest request, String userId) {
        // Verify user
        ApiResponse<UserRepresentation> userResponse = keycloakService.getUserById(userId);
        if (!userResponse.isSuccess() || userResponse.getData() == null) {
            throw new ResourceNotFoundException("User not found");
        }
        var user = userResponse.getData();

        try {

            log.debug("Verifying OTP for {} with operation {}",
                    user.getEmail(), SCHEDULE_TRANSFER_OPERATION);
            // Verify OTP
            Map<String, Object> operationDetails = otpService.verifyOtp(
                    user.getEmail(),
                    SCHEDULE_TRANSFER_OPERATION,
                    request.getOtp());

            if (operationDetails == null) {
                log.error("OTP verification failed for user {} and operation {}",
                        user.getEmail(), SCHEDULE_TRANSFER_OPERATION);
                throw new InvalidOtpException("Invalid or expired verification code");
            }

            // Retrieve pending scheduled transfer
            Cache pendingTransfersCache = cacheManager.getCache(PENDING_SCHEDULED_TRANSFERS_CACHE);
            if (pendingTransfersCache == null) {
                throw new RuntimeException("Scheduled transfer cache not configured");
            }

            Cache.ValueWrapper wrapper = pendingTransfersCache.get(request.getScheduledTransferToken());
            if (wrapper == null) {
                throw new ResourceNotFoundException("Scheduled transfer request expired or not found");
            }

            PendingScheduledTransfer pendingTransfer = (PendingScheduledTransfer) wrapper.get();

            // Verify user ID matches (security check)
            if (!pendingTransfer.getUserId().equals(userId)) {
                throw new IllegalArgumentException("You are not authorized to complete this transfer");
            }

            // Clean up cache
            pendingTransfersCache.evict(request.getScheduledTransferToken());

            // Execute the scheduled transfer
            return scheduleTransfer(pendingTransfer.getRequest(), userId);

        } catch (Exception e) {
            log.error("Error during OTP verification: {}", e.getMessage(), e);
            throw e;
        }

    }

    private String getReceiverName(String email) {
        try {
            UserRepresentation user = keycloakService.getUserByEmail(email);
            if (user != null) {
                return user.getFirstName() + " " + user.getLastName();
            } else {
                return email.split("@")[0];
            }
        } catch (Exception e) {
            log.warn("Error getting receiver name: {}", e.getMessage());
            return email.split("@")[0];
        }
    }

    // Helper class to store pending scheduled transfers
    @Data
    @AllArgsConstructor
    public static class PendingScheduledTransfer {
        private final ScheduledTransferRequestDTO request;
        private final String userId;
    }
}
