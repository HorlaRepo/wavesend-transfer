package com.shizzy.moneytransfer.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Beneficiary {
    private Integer id;
    private String account_number;
    private String bank_code;
    private String full_name;
    private LocalDateTime created_at;
    private String bank_name;
}
