package com.shizzy.moneytransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TransferInitiationResponse {
    private String transferToken;
}