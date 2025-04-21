package com.shizzy.moneytransfer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shizzy.moneytransfer.helpers.ValidatableEntity;
import com.shizzy.moneytransfer.validators.ValidPassword;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Entity(name = "Admin")
@Table(
        name = "admins",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "username_unique",
                        columnNames = "username"
                ),
                @UniqueConstraint(
                        name = "email_unique",
                        columnNames = "email"
                )
        }
)
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Admin implements Serializable {

    @Serial
    private static final long serialVersionUID = 6L;

    @Id
    @SequenceGenerator(
            name = "admin_id_sequence",
            sequenceName = "admin_id_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "admin_id_sequence"
    )
    private Integer adminId;

//    @Column(name = "first_name", nullable = false)
//    @NotBlank(message = "First name cannot be blank")
    private String firstName;

//    @Column(name = "last_name", nullable = false)
//    @NotBlank(message = "Last name cannot be blank")
    private String lastName;

//    @NotBlank(message = "Username field cannot be blank")
    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
//    @ValidPassword()
//    @NotBlank(message = "Password field cannot be blank")
//    @NotNull(message = "Password field cannot be null")
    private String password;

    private LocalDate dateOfBirth;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime lastModifiedDate;

//    @Email(message = "Please provide a valid email address")
//    @NotBlank(message = "Email field cannot be blank")
    private String email;

//    @ManyToMany(fetch = FetchType.EAGER)
//    private Set<UserRole> roles = new HashSet<>();

    @Column
    private boolean accountLocked;
    @Column
    private boolean enabled;


}
