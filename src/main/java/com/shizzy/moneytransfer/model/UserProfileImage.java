package com.shizzy.moneytransfer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class UserProfileImage {
    @Id
    @GeneratedValue
    private Long id;
    @Column(length = 1000)
    private String imageUrl;
    private String createdBy;
}
