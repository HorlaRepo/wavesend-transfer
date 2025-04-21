package com.shizzy.moneytransfer.repository;

import com.shizzy.moneytransfer.model.UserNotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserNotificationPreferencesRepository extends JpaRepository<UserNotificationPreferences, Long> {
    Optional<UserNotificationPreferences> findByCreatedBy(String createdBy);
}
