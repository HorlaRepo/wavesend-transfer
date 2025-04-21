package com.shizzy.moneytransfer.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class WithdrawalData {
    private Integer id;
    private double amount;
    private String account_number;
    private String bank_code;
    private String full_name;
    private String created_at;
    private double fee;
    private String status;
    private String complete_message;
    private int requires_approval;
    private int is_approved;
    private String bank_name;
    private String currency;
    private String reference;
    private String narration;
}
