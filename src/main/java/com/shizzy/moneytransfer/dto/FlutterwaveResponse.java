package com.shizzy.moneytransfer.dto;

import com.shizzy.moneytransfer.model.Bank;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class FlutterwaveResponse {
    private String status;
    private String message;
    private List<Bank> data;
}
