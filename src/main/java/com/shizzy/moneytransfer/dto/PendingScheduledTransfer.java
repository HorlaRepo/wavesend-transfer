package com.shizzy.moneytransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Helper class to store pending scheduled transfers
@Data
@NoArgsConstructor  // Required for Jackson deserialization
@Builder
@AllArgsConstructor
public  class PendingScheduledTransfer implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private  ScheduledTransferRequestDTO request;
    private  String userId;
}
