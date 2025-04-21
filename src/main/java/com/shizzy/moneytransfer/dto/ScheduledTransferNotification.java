package com.shizzy.moneytransfer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.shizzy.moneytransfer.enums.ScheduleStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledTransferNotification {
    private Long scheduledTransferId;
    private String senderEmail;
    private String receiverEmail;
    private Double amount;
    private LocalDateTime scheduledDateTime;
    private ScheduleStatus status;
    private String eventType;
    private LocalDateTime timestamp;
    private String description;
    private String recurrenceType;
    private LocalDateTime recurrenceEndDate;
    private Integer totalOccurrences;
}
