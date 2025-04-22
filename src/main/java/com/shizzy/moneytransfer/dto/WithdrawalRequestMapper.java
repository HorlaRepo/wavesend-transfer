package com.shizzy.moneytransfer.dto;
import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class WithdrawalRequestMapper implements Serializable {
    private static final long serialVersionUID = 1L;
    private double amount;
    private String walletId;
    private double fee;
    private WithdrawalInfo withdrawalInfo;
    private String referenceNumber;
}
