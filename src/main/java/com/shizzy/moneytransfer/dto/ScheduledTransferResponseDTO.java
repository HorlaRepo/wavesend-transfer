package com.shizzy.moneytransfer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shizzy.moneytransfer.enums.RecurrenceType;
import com.shizzy.moneytransfer.enums.ScheduleStatus;

public record ScheduledTransferResponseDTO(
    Long id,
    String senderEmail,
    String receiverEmail,
    BigDecimal amount,

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime scheduledDateTime,
    String description,
    ScheduleStatus status,

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdAt,
    RecurrenceType recurrenceType,

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime recurrenceEndDate,
    Integer totalOccurrences,
    Integer currentOccurrence,
    Long parentTransferId,
    boolean isRecurring
) {
    public boolean isRecurring() {
        return recurrenceType != null && recurrenceType != RecurrenceType.NONE;
    }
}
// The isRecurring method checks if the transfer is recurring by verifying if the recurrenceType is not null and not equal to RecurrenceType.NONE.
// The record also includes a parentTransferId field to link to the original transfer in case of a recurring transfer.