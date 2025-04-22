package com.shizzy.moneytransfer.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.PageResponse;
import com.shizzy.moneytransfer.dto.ScheduledTransferInitiationResponse;
import com.shizzy.moneytransfer.dto.ScheduledTransferRequestDTO;
import com.shizzy.moneytransfer.dto.ScheduledTransferResponseDTO;
import com.shizzy.moneytransfer.dto.ScheduledTransferVerificationRequest;
import com.shizzy.moneytransfer.service.KeycloakService;
import com.shizzy.moneytransfer.service.ScheduledTransferService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("scheduled-transfers")
@RequiredArgsConstructor
@Slf4j
public class ScheduledTransferController {

    private final ScheduledTransferService scheduledTransferService;
    private final KeycloakService keycloakService;

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<ScheduledTransferInitiationResponse>> initiateScheduleTransfer(
            @RequestBody @Valid ScheduledTransferRequestDTO request, Authentication principal) {
        
        log.debug("Initiating scheduled transfer for user: {}", principal.getName());
        return ResponseEntity.ok(
            scheduledTransferService.initiateScheduledTransfer(request, principal.getName())
        );
    }
    
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<ScheduledTransferResponseDTO>> verifyAndScheduleTransfer(
            @RequestBody @Valid ScheduledTransferVerificationRequest request, Authentication principal) {
        
        log.debug("Verifying and scheduling transfer for user: {}", principal.getName());
        return ResponseEntity.ok(
            scheduledTransferService.verifyAndScheduleTransfer(request, principal.getName())
        );
    }
    
    @PostMapping("/schedule")
    public ResponseEntity<ApiResponse<ScheduledTransferResponseDTO>> scheduleTransfer(
            @RequestBody @Valid ScheduledTransferRequestDTO request, Authentication principal) {
        
        log.debug("Scheduling transfer for user: {}", principal.getName());
        return ResponseEntity.ok(scheduledTransferService.scheduleTransfer(request, principal.getName()));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ScheduledTransferResponseDTO>>> getUserTransfers(
            Authentication principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "scheduledDateTime,desc") String[] sort) {
        
        log.debug("Getting paginated scheduled transfers for user: {}", principal.getName());
        
        // Create pageable request with sorting
        Pageable pageable = createPageRequest(page, size, sort);
        
        
        return ResponseEntity.ok(scheduledTransferService.getUserScheduledTransfers(principal.getName(), pageable));
    }
    
    // Legacy endpoint for backward compatibility
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ScheduledTransferResponseDTO>>> getAllUserTransfers(Authentication principal) {
        log.debug("Getting all scheduled transfers for user: {}", principal.getName());
        
        // Get user email to use as cache key
        String userEmail = keycloakService.getUserById(principal.getName()).getData().getEmail();
        
        return ResponseEntity.ok(scheduledTransferService.getUserScheduledTransfers(userEmail));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduledTransferResponseDTO>> cancelTransfer(
            @PathVariable Long id, Authentication principal) {
        
        log.debug("Canceling scheduled transfer {} for user: {}", id, principal.getName());
        
        return ResponseEntity.ok(scheduledTransferService.cancelScheduledTransfer(id, principal.getName()));
    }
    
    @GetMapping("/recurring/{parentId}")
    public ResponseEntity<ApiResponse<List<ScheduledTransferResponseDTO>>> getRecurringSeries(
            @PathVariable Long parentId, Authentication principal) {
        
        log.debug("Getting recurring series {} for user: {}", parentId, principal.getName());
        
        return ResponseEntity.ok(scheduledTransferService.getRecurringTransferSeries(parentId, principal.getName()));
    }
    
    @DeleteMapping("/recurring/{parentId}")
    public ResponseEntity<ApiResponse<ScheduledTransferResponseDTO>> cancelRecurringSeries(
            @PathVariable Long parentId, Authentication principal) {
        
        log.debug("Canceling recurring series {} for user: {}", parentId, principal.getName());
        
        return ResponseEntity.ok(scheduledTransferService.cancelRecurringSeries(parentId, principal.getName()));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduledTransferResponseDTO>> updateTransfer(
            @PathVariable Long id, @RequestBody ScheduledTransferRequestDTO request, Authentication principal) {

        log.debug("Updating scheduled transfer {} for user: {}", id, principal.getName());
        
        String userEmail = keycloakService.getUserById(principal.getName()).getData().getEmail();
        
        // Override sender email with authenticated user
        ScheduledTransferRequestDTO validatedRequest = new ScheduledTransferRequestDTO(
                userEmail, // Use authenticated user's email
                request.receiverEmail(),
                request.amount(),
                request.description(),
                request.scheduledDateTime(),
                request.recurrenceType(),
                request.recurrenceEndDate(),
                request.totalOccurrences()
        );
        
        return ResponseEntity.ok(scheduledTransferService.updateRecurringTransfer(id, validatedRequest, principal.getName()));
    }
    
    /**
     * Creates a PageRequest with sorting parameters
     */
    private Pageable createPageRequest(int page, int size, String[] sort) {
        // Validate page size to prevent excessive load
        if (size > 50) {
            size = 50;
        }
        
        // Create sort criteria
        List<Sort.Order> orders = new ArrayList<>();
        
        if (sort[0].contains(",")) {
            // Format: property,direction
            for (String sortOrder : sort) {
                String[] parts = sortOrder.split(",");
                String property = parts[0];
                
                // Default to ascending if direction is not specified
                Sort.Direction direction = parts.length > 1 ? 
                    "desc".equalsIgnoreCase(parts[1]) ? Sort.Direction.DESC : Sort.Direction.ASC 
                    : Sort.Direction.ASC;
                
                orders.add(new Sort.Order(direction, property));
            }
        } else {
            // Only property is provided, default to ascending
            orders.add(new Sort.Order(Sort.Direction.ASC, sort[0]));
        }
        
        return PageRequest.of(page, size, Sort.by(orders));
    }
}