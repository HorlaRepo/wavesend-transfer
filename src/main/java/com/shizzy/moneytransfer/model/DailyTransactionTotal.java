package com.shizzy.moneytransfer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_transaction_totals", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "date"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyTransactionTotal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private LocalDate date;
    
    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal totalAmount;
    
    /**
     * Add a transaction amount to the daily total
     */
    public void addTransaction(BigDecimal amount) {
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }
        totalAmount = totalAmount.add(amount);
    }
}