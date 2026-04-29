package com.shizzy.moneytransfer.controller;

import com.shizzy.moneytransfer.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyController {

    @GetMapping("/auth-user")
    public String getAuthUser(@AuthenticationPrincipal User user, Authentication connectedUser) {
        return "Hello, " + user.getEmail() + " (" + user.getFullName() + ")\n" +
               "Username: " + connectedUser.getName() + "\n" +
               "Roles: " + user.getRoles();
    }
}
