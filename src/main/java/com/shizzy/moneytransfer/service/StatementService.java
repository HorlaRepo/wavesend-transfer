package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.api.ApiResponse;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;

public interface StatementService {
    ApiResponse<byte[]> generateStatement(Authentication connectedUser, LocalDateTime startDate, LocalDateTime endDate, String format) throws IOException;
}
