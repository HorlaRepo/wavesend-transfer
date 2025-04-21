package com.shizzy.moneytransfer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shizzy.moneytransfer.enums.RecurrenceType;

public record ScheduledTransferRequestDTO(
    @NotNull(message = "Sender email is required")
    @Email(message = "Valid sender email address is required")
    String senderEmail,
    
    @NotNull(message = "Receiver email is required")
    @Email(message = "Valid receiver email address is required")
    String receiverEmail,
    
    @NotNull(message = "Transfer amount is required")
    @Positive(message = "Amount must be greater than zero")
    BigDecimal amount,
    
    String description,
    
    @NotNull(message = "Scheduled date and time is required")
    @Future(message = "Scheduled date and time must be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime scheduledDateTime,
    
    RecurrenceType recurrenceType,
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime recurrenceEndDate,
    
    @Min(value = 1, message = "Total occurrences must be at least 1")
    Integer totalOccurrences
) {}