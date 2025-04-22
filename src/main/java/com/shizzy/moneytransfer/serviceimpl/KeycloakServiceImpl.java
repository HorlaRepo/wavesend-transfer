package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.service.KeycloakService;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KeycloakServiceImpl  implements KeycloakService {

    private final UsersResource usersResource;

    @Override
    public ApiResponse<UserRepresentation> existsUserByEmail(String email) {
        List<UserRepresentation> users = usersResource.search(null, null, null, email, null, null);
        if (users != null && !users.isEmpty()) {
            return ApiResponse.<UserRepresentation>builder()
                    .success(true)
                    .message("User found")
                    .data(users.get(0))
                    .build();
        }

        return ApiResponse.<UserRepresentation>builder()
                .success(false)
                .data(null)
                .message("User not found")
                .build();
    }

    @Override
    public ApiResponse<UserRepresentation> getUserById(String userId) {
        UserRepresentation user = usersResource.get(userId).toRepresentation();
        return ApiResponse.<UserRepresentation>builder()
                .success(true)
                .message("User found")
                .data(user)
                .build();
    }

    @Override
    public UserRepresentation getUserByEmail(String email) {
        return usersResource.search(null, null, null, email, null, null).get(0);
    }

    @Override
    public String getUserFullName(String email) {
        UserRepresentation user = usersResource.search(null, null, null, email, null, null).get(0);
        return user.getFirstName() + " " + user.getLastName();
    }

    @Override
    public String getUserId(String email) {
        UserRepresentation user = usersResource.search(null, null, null, email, null, null).get(0);
        return user.getId();
    }

    @Override
    public ApiResponse<Void> resetPassword(String userId, String newPassword) {
        try {
            // Create a credential representation
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(newPassword);
            credential.setTemporary(false);

            // Reset password
            usersResource.get(userId).resetPassword(credential);

            return ApiResponse.<Void>builder()
                    .success(true)
                    .message("Password reset successfully")
                    .build();
        } catch (Exception e) {
            return ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to reset password: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ApiResponse<Void> sendPasswordResetEmail(String email) {
        try {
            List<UserRepresentation> users = usersResource.search(null, null, null, email, null, null);
            if (users == null || users.isEmpty()) {
                return ApiResponse.<Void>builder()
                        .success(false)
                        .message("User not found with email: " + email)
                        .build();
            }
    
            // This will send a password reset email through Keycloak
            usersResource.get(users.get(0).getId()).executeActionsEmail(List.of("UPDATE_PASSWORD"));
    
            return ApiResponse.<Void>builder()
                    .success(true)
                    .message("Password reset email sent successfully")
                    .build();
        } catch (Exception e) {
            return ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to send password reset email: " + e.getMessage())
                    .build();
        }
    }
}
