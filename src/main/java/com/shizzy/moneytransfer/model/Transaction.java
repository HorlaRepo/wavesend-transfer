package com.shizzy.moneytransfer.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shizzy.moneytransfer.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "transactions", uniqueConstraints = {
                @UniqueConstraint(name = "mtcn_unique", columnNames = "mtcn")
})
@Builder
public class Transaction implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Id
        @SequenceGenerator(name = "transaction_id_sequence", sequenceName = "transaction_id_sequence", allocationSize = 1)
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transaction_id_sequence")
        @Column(name = "transaction_id", columnDefinition = "BIGSERIAL")
        private Integer transactionId;

        private String providerId;

        @Column(nullable = false)
        private BigDecimal amount;

        @Builder.Default
        private BigDecimal refundableAmount = BigDecimal.ZERO;

        private String mtcn;

        @Column
        private String currentStatus;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        @Column(nullable = false)
        private LocalDateTime transactionDate;

        @JsonIgnore
        @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "origin")
        private Country origin;

        @JsonIgnore
        @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "destination")
        private Country destination;

        private String referenceNumber;
        private String description;
        @Column
        private String narration;

        @Column
        private String failureReason;

        @Column
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime completedAt;

        @Column
        private double fee;

        @Enumerated(EnumType.STRING)
        @Column
        private TransactionOperation operation;

        @Enumerated(EnumType.STRING)
        private SendingMethod sendingMethod;

        @Enumerated(EnumType.STRING)
        private DeliveryMethod deliveryMethod;

        @ManyToOne
        @JoinColumn(name = "wallet_id")
        private Wallet wallet;

        @Enumerated(EnumType.STRING)
        private TransactionType transactionType;

        @Column(nullable = true)
        private String sessionId;

        @OneToOne(cascade = CascadeType.ALL)
        @JoinColumn(name = "security_question_id", referencedColumnName = "id")
        private SecurityQuestion securityQuestion;

        @Column(columnDefinition = "boolean default false")
        private boolean flagged;

        @Builder.Default
        @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<FlaggedTransactionReason> flaggedTransactionReasons = new ArrayList<>(); // Initialize here

        // Add these helper methods
        public void addFlaggedReason(FlaggedTransactionReason reason) {
                if (flaggedTransactionReasons == null) {
                        flaggedTransactionReasons = new ArrayList<>();
                }
                flaggedTransactionReasons.add(reason);
                reason.setTransaction(this);
        }

        public void clearFlaggedReasons() {
                if (flaggedTransactionReasons != null) {
                        flaggedTransactionReasons.forEach(reason -> reason.setTransaction(null));
                        flaggedTransactionReasons.clear();
                }
        }

        @Enumerated(EnumType.STRING)
        private TransactionSource source;

        @Enumerated(EnumType.STRING)
        private RefundStatus refundStatus;

        // @JsonIgnore
        // @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
        // @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, fetch =
        // FetchType.LAZY)
        // private List<TransactionStatus> transactionStatuses;
}
