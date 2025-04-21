package com.shizzy.moneytransfer.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private String path;
    private String message;
    private int statusCode;
    private String description;
    private Set<String> validationErrors;
    private LocalDateTime localDateTime;

}