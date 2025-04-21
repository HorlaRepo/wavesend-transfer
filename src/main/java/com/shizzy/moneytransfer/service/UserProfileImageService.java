package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.exception.InvalidFileFormatException;
import com.shizzy.moneytransfer.model.UserProfileImage;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

public interface UserProfileImageService {
    void updateUserProfileImage(String profileImageUrl, Authentication connectedUser);
    byte[] getUserProfileImage(Authentication connectedUser);
    ApiResponse<String> uploadUserProfileImage(Authentication connectedUser, MultipartFile userProfileImage) throws IOException, InvalidFileFormatException;
    ApiResponse<String> getUserProfileImageUrl(Authentication connectedUser);
}
