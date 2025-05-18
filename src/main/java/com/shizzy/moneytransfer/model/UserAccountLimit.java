package com.shizzy.moneytransfer.model;

import com.shizzy.moneytransfer.enums.VerificationLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

@Entity
@Table(name = "user_account_limits")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAccountLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationLevel verificationLevel;
    
    @Column(nullable = false)
    @CreatedDate
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}