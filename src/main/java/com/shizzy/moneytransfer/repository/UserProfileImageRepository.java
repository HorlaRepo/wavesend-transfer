package com.shizzy.moneytransfer.repository;

import com.shizzy.moneytransfer.model.UserProfileImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserProfileImageRepository extends JpaRepository<UserProfileImage, Long> {
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE UserProfileImage u SET u.imageUrl = ?1 WHERE u.createdBy = ?2")
    void updateUserProfileImage(String profileImageUrl, String createdBy);

    Optional<UserProfileImage> findByCreatedBy(String createdBy);
}
