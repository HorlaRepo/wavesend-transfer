package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.UserRepresentation;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.User;
import com.shizzy.moneytransfer.repository.UserRepository;
import com.shizzy.moneytransfer.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public ApiResponse<List<User>> getAllUsers(Integer page, Integer pageSize) {
        Page<User> users = userRepository.findAll(PageRequest.of(page, pageSize));
        return ApiResponse.<List<User>>builder()
                .success(true)
                .message("Users retrieved successfully")
                .data(users.getContent())
                .build();
    }

    @Override
    public ApiResponse<User> getUserById(Integer id) {
        User user = userRepository.findById(UUID.fromString(String.valueOf(id)))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return ApiResponse.<User>builder()
                .success(true)
                .message("User found")
                .data(user)
                .build();
    }

    @Override
    public ApiResponse<User> getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return ApiResponse.<User>builder()
                .success(true)
                .message("User found")
                .data(user)
                .build();
    }

    @Override
    public ApiResponse<UserRepresentation> checkUserByEmail(String email) {
        log.debug("Checking if user exists with email: {}", email);

        return userRepository.findByEmail(email)
                .map(user -> {
                    UserRepresentation userRep = UserRepresentation.builder()
                            .id(String.valueOf(user.getUserId()))
                            .username(user.getEmail())
                            .email(user.getEmail())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .emailVerified(true)
                            .enabled(user.isEnabled())
                            .build();

                    return ApiResponse.<UserRepresentation>builder()
                            .success(true)
                            .message("User found")
                            .data(userRep)
                            .build();
                })
                .orElseGet(() -> ApiResponse.<UserRepresentation>builder()
                        .success(false)
                        .message("User not found with email: " + email)
                        .build());
    }

    @Override
    public void deleteUserById(Integer id) {
        User user = userRepository.findById(UUID.fromString(String.valueOf(id)))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userRepository.delete(user);
    }

    @Override
    public void checkIfUserExistsOrThrow(Integer id) {
        if (!userRepository.existsById(UUID.fromString(String.valueOf(id)))) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
    }

    @Override
    public User findUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
