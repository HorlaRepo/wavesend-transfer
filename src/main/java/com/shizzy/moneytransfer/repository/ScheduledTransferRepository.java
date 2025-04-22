package com.shizzy.moneytransfer.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.shizzy.moneytransfer.enums.ScheduleStatus;
import com.shizzy.moneytransfer.model.ScheduledTransfer;

@Repository
public interface ScheduledTransferRepository extends JpaRepository<ScheduledTransfer, Long> {
    
    List<ScheduledTransfer> findBySenderEmailOrderByScheduledDateTimeDesc(String senderEmail);
    
    Page<ScheduledTransfer> findBySenderEmailOrderByScheduledDateTimeDesc(String senderEmail, Pageable pageable);
    Page<ScheduledTransfer> findByCreatedByOrderByScheduledDateTimeDesc(String userId, Pageable pageable);
        
    // Find transfers by status and scheduled time
    List<ScheduledTransfer> findByStatusAndScheduledDateTimeLessThanEqual(
            ScheduleStatus status, LocalDateTime dateTime);
    
    Page<ScheduledTransfer> findByStatusAndScheduledDateTimeLessThanEqual(
            ScheduleStatus status, LocalDateTime dateTime, Pageable pageable);
    
    // Find recurring transfers
    List<ScheduledTransfer> findByParentTransferId(Long parentId);
    
    List<ScheduledTransfer> findByParentTransferIdAndStatus(Long parentId, ScheduleStatus status);
    
    // New methods for Kafka-based processing
    
    // Find pending transfers that are due but not processed yet
    @Query("SELECT t FROM ScheduledTransfer t WHERE t.status = :status AND t.scheduledDateTime <= :dateTime AND t.processed = false")
    List<ScheduledTransfer> findDueUnprocessedTransfers(
            @Param("status") ScheduleStatus status, 
            @Param("dateTime") LocalDateTime dateTime);
    
    // Find pending transfers that are due within a specific time window
    @Query("SELECT t FROM ScheduledTransfer t WHERE t.status = :status " +
           "AND t.scheduledDateTime BETWEEN :startTime AND :endTime " +
           "AND t.processed = false")
    List<ScheduledTransfer> findDueUnprocessedTransfersInTimeWindow(
            @Param("status") ScheduleStatus status, 
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    // Find failed transfers for retry
    @Query("SELECT t FROM ScheduledTransfer t WHERE t.status = :status " +
           "AND t.retryCount < :maxRetries " +
           "AND (t.lastRetryDateTime IS NULL OR t.lastRetryDateTime <= :retryBefore)")
    List<ScheduledTransfer> findFailedTransfersForRetry(
            @Param("status") ScheduleStatus status,
            @Param("maxRetries") int maxRetries,
            @Param("retryBefore") LocalDateTime retryBefore);
            
    // Find transfers that have been processed by the Kafka system
    List<ScheduledTransfer> findByProcessedTrue();
    
    // Find overdue but unprocessed transfers (those we missed while server was down)
    @Query("SELECT t FROM ScheduledTransfer t WHERE t.status = :status " +
           "AND t.scheduledDateTime < :currentTime " +
           "AND t.processed = false " +
           "ORDER BY t.scheduledDateTime ASC")
    List<ScheduledTransfer> findOverdueUnprocessedTransfers(
            @Param("status") ScheduleStatus status,
            @Param("currentTime") LocalDateTime currentTime,
            Pageable pageable);

    List<ScheduledTransfer> findByCreatedBy(String userId);
}
