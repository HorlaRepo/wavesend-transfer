package com.shizzy.moneytransfer.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class FeeData {
    private String fee_type;
    private String currency;
    private double fee;
}
