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
public class ScheduledTransferMessage {
    private Long transferId;
    private String senderEmail;
    private String receiverEmail;
    private BigDecimal amount;
    private String description;
    private LocalDateTime scheduledDateTime;
    private ScheduleStatus status;
    private LocalDateTime messageTimestamp;
    private Long parentTransferId;
    private boolean isRecurring;
    private String messageType; // PUBLISH, RETRY, RECOVERY
}