//package com.shizzy.moneytransfer.model;
//
//import com.fasterxml.jackson.annotation.JsonIgnore;
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import com.fasterxml.jackson.annotation.JsonManagedReference;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.shizzy.moneytransfer.enums.Gender;
//import com.shizzy.moneytransfer.helpers.ValidatableEntity;
//import com.shizzy.moneytransfer.validators.ValidPassword;
//import jakarta.persistence.*;
//import jakarta.validation.constraints.Email;
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.NotNull;
//import lombok.*;
//import org.springframework.data.annotation.CreatedDate;
//import org.springframework.data.annotation.LastModifiedDate;
//import org.springframework.data.jpa.domain.support.AuditingEntityListener;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//
//import java.io.Serial;
//import java.io.Serializable;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.Set;
//
//@Getter
//@Setter
//@AllArgsConstructor
//@NoArgsConstructor
////@Entity(name = "users")
////@EntityListeners(AuditingEntityListener.class)
////@Table(
////        name = "users"
////)
//
////@Builder
//public class User extends BaseEntity implements Serializable, ValidatableEntity {
//
//    @Serial
//    private static final long serialVersionUID = 1L;
//
//    @Id
//    @SequenceGenerator(
//            name = "users_id_sequence",
//            sequenceName = "users_id_sequence",
//            allocationSize = 1
//    )
//    @GeneratedValue(
//            strategy = GenerationType.SEQUENCE,
//            generator = "users_id_sequence"
//    )
//    @Column(name = "users_id", columnDefinition = "BIGSERIAL")
//    private Integer userId;
//
//    @Column(name = "first_name", nullable = false)
////    @NotBlank(message = "First name cannot be blank")
//    private String firstName;
//
//    @Column(name = "last_name", nullable = false)
//    private String lastName;
//
//    @Column(name = "email", nullable = false, unique = true)
//    private String email;
//
//    private LocalDate dateOfBirth;
//
//    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
//    @ValidPassword()
//    @NotBlank(message = "Password field cannot be blank")
//    @NotNull(message = "Password field cannot be null")
//    private String password;
//
//    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
//    @JoinColumn(name = "wallet_id", referencedColumnName = "id")
//    private Wallet wallet;
//
//    private String phoneNumber;
//
//    @Column
//    @Enumerated(EnumType.STRING)
//    private Gender gender;
//
//    private boolean accountLocked;
//    private boolean enabled;
//
//    @Column(nullable = false)
//    @Embedded
//    private Address address;
//
//    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
//    @JsonIgnore
//    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
//    @JsonManagedReference
//    private Set<BankAccount> bankAccounts;
//
//
//    @Override
//    public String getFullName() {
//        return firstName + " " + lastName;
//    }
//
//    @Override
//    public User user() {
//        return this;
//    }
//
//    @Override
//    public Admin admin() {
//        return null;
//    }
//}
