//package com.shizzy.moneytransfer.model;
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.time.Instant;
//import java.time.LocalDateTime;
//
////@Entity
////@Data
////@AllArgsConstructor
////@NoArgsConstructor
////@Builder
////@Table(name = "Token")
//public class Token {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private int id;
//    private String token;
//
//    private LocalDateTime createdAt;
//    private LocalDateTime expiresAt;
//    private LocalDateTime validatedAt;
//
////    @ManyToOne
////    @JoinColumn(name = "user_id")
////    private User user;
//
//    @ManyToOne
//    @JoinColumn(name = "admin_id")
//    private Admin admin;
//
//}
