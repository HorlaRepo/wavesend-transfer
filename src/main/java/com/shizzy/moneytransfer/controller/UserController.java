//package com.shizzy.moneytransfer.controller;
//
//import com.shizzy.moneytransfer.api.ApiResponse;
//import com.shizzy.moneytransfer.model.User;
//import com.shizzy.moneytransfer.service.UserService;
//import com.shizzy.moneytransfer.serviceimpl.UserServiceImpl;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN','ROLE_USER')")
//@RequestMapping("users")
//@RequiredArgsConstructor
//public class UserController {
//
//    private final UserService userService;
//
//    @GetMapping
//    public ApiResponse<List<User>> getUsers(
//            @RequestParam(defaultValue = "0", required = false) Integer page,
//            @RequestParam (defaultValue = "200", required = false) Integer pageSize) {
//        return userService.getAllUsers(page, pageSize);
//    }
//
//    @GetMapping("{id}")
//    public ApiResponse<User> getUser(@PathVariable("id") Integer id) {
//        return userService.getUserById(id);
//    }
//
//    @GetMapping("email/{email}")
//    public ApiResponse<User> getUserByEmail(@PathVariable("email") String email) {
//        return userService.getUserByEmail(email);
//    }
//
//}
