package com.shizzy.moneytransfer.controller;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.exception.InvalidFileFormatException;
import com.shizzy.moneytransfer.model.UserProfileImage;
import com.shizzy.moneytransfer.service.UserProfileImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("user-profile-image")
@RequiredArgsConstructor
@Slf4j
public class UserProfileImageController {

    private final UserProfileImageService userProfileImageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> uploadUserProfileImage(Authentication connectedUser, @RequestParam("file") MultipartFile file)  {
        try {
            return userProfileImageService.uploadUserProfileImage(connectedUser, file);
        } catch (IOException | InvalidFileFormatException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping(value = "/get", produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] getUserProfileImage(Authentication connectedUser) {
        return userProfileImageService.getUserProfileImage(connectedUser);
    }

    @GetMapping(value = "/url", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<String> getUserProfileImageUrl(Authentication connectedUser) {
        log.info("Fetching profile image URL for user: {}", connectedUser.getName());
        return userProfileImageService.getUserProfileImageUrl(connectedUser);
    }

    //, produces = MediaType.IMAGE_JPEG_VALUE

}
