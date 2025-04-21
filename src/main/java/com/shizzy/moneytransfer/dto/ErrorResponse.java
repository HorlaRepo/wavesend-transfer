package com.shizzy.moneytransfer.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ErrorResponse {
    private boolean status;
    private String message;
    private MetaData meta;
    private String type;
    private String code;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class MetaData {
        private String nextStep;
    }
}