package com.shizzy.moneytransfer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cardType;
    @Column(unique = true)
    private String cardNumber;
    private String expiryDate;
    private String cvv;
    private String cardName;

    @Column
    private boolean isLocked;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column()
    private String pin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

}