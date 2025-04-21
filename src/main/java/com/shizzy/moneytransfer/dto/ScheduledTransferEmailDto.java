package com.shizzy.moneytransfer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.shizzy.moneytransfer.enums.RecurrenceType;
import com.shizzy.moneytransfer.enums.ScheduleStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledTransferEmailDto {
    private Long transferId;
    private String senderEmail;
    private String receiverEmail;
    private String senderName;
    private String receiverName;
    private BigDecimal amount;
    private String description;
    private LocalDateTime scheduledDateTime;
    private LocalDateTime executionDateTime;
    private LocalDateTime cancellationDateTime;
    private LocalDateTime failureDateTime;
    private ScheduleStatus status;
    private RecurrenceType recurrenceType;
    private LocalDateTime recurrenceEndDate;
    private Integer currentOccurrence;
    private Integer totalOccurrences;
    private LocalDateTime nextScheduledDate;
    private String subject;
    private boolean isRecurring;
}