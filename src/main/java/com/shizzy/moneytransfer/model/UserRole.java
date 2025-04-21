//package com.shizzy.moneytransfer.model;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.io.Serial;
//import java.io.Serializable;
//
//@Entity
//@Data
//@ToString
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Table(name = "Roles")
//
//public class UserRole implements Serializable {
//
//    @Serial
//    private static final long serialVersionUID = 7L;
//
//    @Id
//    @SequenceGenerator(
//            name = "roles_id_sequence",
//            sequenceName = "roles_id_sequence",
//            allocationSize = 1
//    )
//    @GeneratedValue(
//            strategy = GenerationType.SEQUENCE,
//            generator = "roles_id_sequence"
//    )
//    @Column(name = "roles_id", columnDefinition = "BIGSERIAL")
//    private Integer id;
//    private String name;
//
//}
