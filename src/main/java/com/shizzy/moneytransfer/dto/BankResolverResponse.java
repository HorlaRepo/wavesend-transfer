package com.shizzy.moneytransfer.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BankResolverResponse {
    private boolean status;
    private String message;
    private AccountData data;
}
