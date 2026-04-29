package com.shizzy.moneytransfer.controller;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.UserRepresentation;
import com.shizzy.moneytransfer.model.User;
import com.shizzy.moneytransfer.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN','ROLE_USER')")
    @GetMapping
    public ApiResponse<List<User>> getUsers(
            @RequestParam(defaultValue = "0", required = false) Integer page,
            @RequestParam (defaultValue = "200", required = false) Integer pageSize) {
        return userService.getAllUsers(page, pageSize);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN','ROLE_USER')")
    @GetMapping("{id}")
    public ApiResponse<User> getUser(@PathVariable("id") Integer id) {
        return userService.getUserById(id);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN','ROLE_USER')")
    @GetMapping("email/{email}")
    public ApiResponse<User> getUserByEmail(@PathVariable("email") String email) {
        return userService.getUserByEmail(email);
    }

    // Replacement for Keycloak checkUser endpoint
    @GetMapping("/check-user")
    public ApiResponse<UserRepresentation> checkUser(@RequestParam String email) {
        return userService.checkUserByEmail(email);
    }

}
