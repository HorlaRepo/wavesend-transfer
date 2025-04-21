package com.shizzy.moneytransfer.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BeneficiaryAiSuggestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String userId;
    
    private Long beneficiaryId;
    
    private String beneficiaryName;
    
    private BigDecimal suggestedAmount;
    
    @Column(length = 1000)
    private String suggestionText;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime expiresAt;
    
    private boolean seen;
    
    private boolean dismissed;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusDays(1); 
        }
    }
}