package com.shizzy.moneytransfer.model;

import com.shizzy.moneytransfer.enums.RefundImpactType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refund_impact_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundImpactRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Integer depositTransactionId;
    
    @Enumerated(EnumType.STRING)
    private RefundImpactType impactType;
    
    private BigDecimal impactAmount;
    
    private BigDecimal previousRefundableAmount;
    
    private BigDecimal newRefundableAmount;
    
    private LocalDateTime impactDate;
    
    private BigDecimal relatedTransferAmount;
    
    private String notes;
}