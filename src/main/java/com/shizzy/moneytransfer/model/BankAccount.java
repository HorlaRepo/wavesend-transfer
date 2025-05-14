package com.shizzy.moneytransfer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "bank_accounts")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BankAccount extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String bankName;
    private String accountNumber;
    private String accountName;
    private String accountType;
    private String currency;
    private String bankCountry;
    private String bankCode;
    private String region;
    private String swiftCode;
    private String routingNumber;
    private String beneficiaryName;
    private String beneficiaryAddress;
    private String beneficiaryCountry;
    private String postalCode;
    private String streetNumber;
    private String streetName;
    private String city;
    private String paymentMethod;
}
