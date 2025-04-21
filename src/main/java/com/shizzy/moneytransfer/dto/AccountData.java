package com.shizzy.moneytransfer.dto;

import lombok.Data;

@Data
public class AccountData {
    private String account_number;
    private String account_name;
    private int bank_id;
}
