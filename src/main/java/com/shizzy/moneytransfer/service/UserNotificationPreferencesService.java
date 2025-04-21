package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.UserNotificationPreferencesRequest;
import com.shizzy.moneytransfer.model.UserNotificationPreferences;
import org.springframework.security.core.Authentication;

public interface UserNotificationPreferencesService {
    ApiResponse<UserNotificationPreferences> updateNotificationPreferences(Authentication connectedUser, UserNotificationPreferencesRequest userNotificationPreferences);
    ApiResponse<UserNotificationPreferences> getNotificationPreferences(Authentication connectedUser);
    void setDefNotificationPreferences(String userId);
}
