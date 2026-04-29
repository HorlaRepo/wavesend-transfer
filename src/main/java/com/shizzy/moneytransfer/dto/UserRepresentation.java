package com.shizzy.moneytransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRepresentation {
    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private boolean emailVerified;
    private boolean enabled;
}
