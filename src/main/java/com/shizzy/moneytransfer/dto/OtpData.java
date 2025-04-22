package com.shizzy.moneytransfer.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpData implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private  String otp;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private  LocalDateTime createdAt;
    
    private  Map<String, Object> operationDetails;
}