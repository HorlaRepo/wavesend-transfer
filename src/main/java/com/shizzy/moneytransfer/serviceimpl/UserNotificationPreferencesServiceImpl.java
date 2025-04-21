package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.UserNotificationPreferencesRequest;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.UserNotificationPreferences;
import com.shizzy.moneytransfer.repository.UserNotificationPreferencesRepository;
import com.shizzy.moneytransfer.service.UserNotificationPreferencesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import static org.slf4j.LoggerFactory.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserNotificationPreferencesServiceImpl implements UserNotificationPreferencesService {

    private final Logger logger = getLogger(UserNotificationPreferencesServiceImpl.class);

    private final UserNotificationPreferencesRepository preferencesRepository;

    @Override
    public ApiResponse<UserNotificationPreferences> updateNotificationPreferences(Authentication connectedUser, UserNotificationPreferencesRequest userNotificationPreferences) {

        logger.info("Notification settings request: {}", userNotificationPreferences);
        UserNotificationPreferences preferences = preferencesRepository.findByCreatedBy(connectedUser.getName()).orElse(
                UserNotificationPreferences.builder()
                        .createdBy(connectedUser.getName())
                        .notifyOnDeposit(userNotificationPreferences.notifyOnDeposit())
                        .notifyOnWithdraw(userNotificationPreferences.notifyOnWithdraw())
                        .notifyOnSend(userNotificationPreferences.notifyOnSend())
                        .notifyOnReceive(userNotificationPreferences.notifyOnReceive())
                        .notifyOnPaymentFailure(userNotificationPreferences.notifyOnPaymentFailure())
                        .build());

        preferences.setNotifyOnDeposit(userNotificationPreferences.notifyOnDeposit());
        preferences.setNotifyOnWithdraw(userNotificationPreferences.notifyOnWithdraw());
        preferences.setNotifyOnSend(userNotificationPreferences.notifyOnSend());
        preferences.setNotifyOnReceive(userNotificationPreferences.notifyOnReceive());
        preferences.setNotifyOnPaymentFailure(userNotificationPreferences.notifyOnPaymentFailure());

        return ApiResponse.<UserNotificationPreferences>builder()
                .data(preferencesRepository.save(preferences))
                .success(true)
                .message("Notification preferences updated successfully")
                .build();
    }

    @Override
    public ApiResponse<UserNotificationPreferences> getNotificationPreferences(Authentication connectedUser) {

        UserNotificationPreferences preferences = preferencesRepository.findByCreatedBy(connectedUser.getName()).orElseThrow(
                ()-> new ResourceNotFoundException("Notification preferences not found for user"));

        return ApiResponse.<UserNotificationPreferences>builder()
                .data(preferences)
                .success(true)
                .message("Notification preferences retrieved successfully")
                .build();
    }

    @Override
    public void setDefNotificationPreferences(String userId) {

        UserNotificationPreferences preferences = UserNotificationPreferences
        .builder().createdBy(userId)
        .notifyOnDeposit(true)
        .notifyOnWithdraw(true)
        .notifyOnSend(true)
        .notifyOnReceive(true)
        .notifyOnPaymentFailure(true)
        .notifyOnScheduledTransfers(true)
        .notifyOnExecutedTransfers(true)
        .notifyOnCancelledTransfers(true)
        .build();

        preferencesRepository.save(preferences);

        logger.info("Default notification preferences set for user: {}", userId);
    }

    
}
