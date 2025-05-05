package com.shizzy.moneytransfer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shizzy.moneytransfer.enums.RecurrenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScheduledTransferDetails {
    private BigDecimal amount;
    private String recipientName;
    private LocalDateTime scheduledDateTime;
    private RecurrenceType recurrenceType;
    private LocalDateTime recurrenceEndDate;
    private Integer totalOccurrences;
    private String note;
}