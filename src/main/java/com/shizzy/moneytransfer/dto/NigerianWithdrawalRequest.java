package com.shizzy.moneytransfer.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class NigerianWithdrawalRequest extends AfricaBaseWithdrawalRequest {
    private String name;
}
