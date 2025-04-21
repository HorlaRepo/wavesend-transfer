package com.shizzy.moneytransfer.dto;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class WithdrawalRequestMapper {
    private double amount;
    private String walletId;
    private double fee;
    private WithdrawalInfo withdrawalInfo;
    private String referenceNumber;
}
