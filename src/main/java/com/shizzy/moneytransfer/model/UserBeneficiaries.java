package com.shizzy.moneytransfer.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserBeneficiaries {

    @Id
    private String userId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "userId", nullable = false)
    @Builder.Default
    private List<UserBeneficiary> beneficiaries = new ArrayList<>();
}