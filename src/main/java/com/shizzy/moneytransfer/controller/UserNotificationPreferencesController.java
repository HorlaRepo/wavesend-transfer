package com.shizzy.moneytransfer.controller;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.UserNotificationPreferencesRequest;
import com.shizzy.moneytransfer.model.UserNotificationPreferences;
import com.shizzy.moneytransfer.service.UserNotificationPreferencesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("user-notification-preferences")
@RequiredArgsConstructor
public class UserNotificationPreferencesController {

    private final UserNotificationPreferencesService userNotificationPreferencesService;

    @PutMapping("/update")
    public ApiResponse<UserNotificationPreferences> updateNotificationPreferences(Authentication connectedUser, @RequestBody @Valid UserNotificationPreferencesRequest userNotificationPreferences){
        return userNotificationPreferencesService.updateNotificationPreferences(connectedUser, userNotificationPreferences);
    }

    @GetMapping
    public ApiResponse<UserNotificationPreferences> getNotificationPreferences(Authentication connectedUser){
        return userNotificationPreferencesService.getNotificationPreferences(connectedUser);
    }
}
