package com.shizzy.moneytransfer.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Setter
@Getter
@Builder
public class TransactionReference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = true)
    @JoinColumn(nullable = true)
    private Transaction debitTransaction;

    @OneToOne(optional = true)
    @JoinColumn(nullable = true)
    private Transaction creditTransaction;

    private String referenceNumber;
}
