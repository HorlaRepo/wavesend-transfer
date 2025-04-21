package com.shizzy.moneytransfer.controller;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.service.StatementService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/statements")
@RequiredArgsConstructor
public class StatementController {

    private final StatementService statementService;

    @GetMapping("/generate")
    public ResponseEntity<?> generateStatement(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam("format") String format,
            Authentication connectedUser) throws IOException {

        // Call the service to generate the statement
        ApiResponse<byte[]> apiResponse = statementService.generateStatement(connectedUser, startDate, endDate, format);

        if (apiResponse.isSuccess()) {
            // Prepare headers for file download
            String contentType = format.equalsIgnoreCase("csv") ? "text/csv" : "application/pdf";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", "statement." + format.toLowerCase());

            // Return the byte array with headers for successful response
            return new ResponseEntity<>(apiResponse.getData(), headers, HttpStatus.OK);
        } else {
            // Return error message for failed response
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse.getMessage());
        }
    }
}
