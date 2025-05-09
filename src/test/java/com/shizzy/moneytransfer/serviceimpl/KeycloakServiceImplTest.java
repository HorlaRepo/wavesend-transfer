package com.shizzy.moneytransfer.serviceimpl;



import com.shizzy.moneytransfer.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;





@ExtendWith(MockitoExtension.class)
class KeycloakServiceImplTest {

    @Mock
    private UsersResource usersResource;

    @InjectMocks
    private KeycloakServiceImpl keycloakService;

    @Test
    void existsUserByEmail_whenUserExists_returnSuccessResponse() {
        // Arrange
        String email = "test@example.com";
        UserRepresentation mockUser = new UserRepresentation();
        mockUser.setEmail(email);
        mockUser.setFirstName("Test");
        mockUser.setLastName("User");
        
        when(usersResource.search(null, null, null, email, null, null))
                .thenReturn(List.of(mockUser));

        // Act
        ApiResponse<UserRepresentation> response = keycloakService.existsUserByEmail(email);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("User found", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(mockUser, response.getData());
        verify(usersResource).search(null, null, null, email, null, null);
    }

    @Test
    void existsUserByEmail_whenNoUserExists_returnFailureResponse() {
        // Arrange
        String email = "nonexistent@example.com";
        
        when(usersResource.search(null, null, null, email, null, null))
                .thenReturn(Collections.emptyList());

        // Act
        ApiResponse<UserRepresentation> response = keycloakService.existsUserByEmail(email);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("User not found", response.getMessage());
        assertNull(response.getData());
        verify(usersResource).search(null, null, null, email, null, null);
    }

    @Test
    void existsUserByEmail_whenUsersListIsNull_returnFailureResponse() {
        // Arrange
        String email = "test@example.com";
        
        when(usersResource.search(null, null, null, email, null, null))
                .thenReturn(null);

        // Act
        ApiResponse<UserRepresentation> response = keycloakService.existsUserByEmail(email);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("User not found", response.getMessage());
        assertNull(response.getData());
        verify(usersResource).search(null, null, null, email, null, null);
    }
}