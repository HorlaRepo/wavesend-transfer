package com.shizzy.moneytransfer.api;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GenericResponse <T>{
    private String status;
    private String message;
    private T data;
}
