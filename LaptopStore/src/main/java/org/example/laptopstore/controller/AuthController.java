package org.example.laptopstore.controller;

import lombok.RequiredArgsConstructor;

import org.example.laptopstore.dto.request.account.*;
import org.example.laptopstore.dto.response.ApiResponse;

import org.example.laptopstore.dto.response.account.LoginResponse;
import org.example.laptopstore.dto.response.account.RegisterReponse;
import org.example.laptopstore.service.UserAccountService;
import org.example.laptopstore.service.impl.EmailService;
import org.example.laptopstore.util.Validation;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

import static org.example.laptopstore.util.Constant.PASSWORD_CHANGE_SUCESSFUL;
import static org.example.laptopstore.util.Constant.SUCCESS;
import static org.example.laptopstore.util.Constant.SUCCESS_MESSAGE;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final EmailService emailService;
    private final UserAccountService userService;
    @PostMapping("/forgot-password")
    public ApiResponse<Object> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        userService.forgotPassword(request.getEmail());
        return ApiResponse.builder()
                .message("Mã OTP đã được gửi đến email của bạn")
                .code(SUCCESS)
                .build();
    }

    @PostMapping("/reset-password")
    public ApiResponse<Object> resetPassword(@RequestBody ResetPasswordRequest request) {

        // Validate mật khẩu mới
        if (!Validation.isValidPassword(request.getNewPassword())) {
            return ApiResponse.builder()
                    .message("Mật khẩu phải ít nhất 8 ký tự, bao gồm chữ, số và ký tự đặc biệt")
                    .code(400)
                    .build();
        }

        userService.resetPassword(request);
        return ApiResponse.builder()
                .message("Đặt lại mật khẩu thành công")
                .code(SUCCESS)
                .build();
    }

    // ------------------ ĐĂNG KÝ TÀI KHOẢN ------------------
    @PostMapping("/register/request-otp")
    public ApiResponse<Object> requestOtp(@RequestBody RegisterRequest request) {
        if (userService.existsByEmail(request.getEmail())) {
            return ApiResponse.builder()
                    .code(400)
                    .message("Email đã được sử dụng")
                    .build();
        }

        String otp = userService.generateOtp(request.getEmail());
        emailService.sendOtp(request.getEmail(), otp);

        return ApiResponse.builder()
                .code(200)
                .message("Đã gửi OTP đến email của bạn")
                .build();
    }

    @PostMapping("/register/verify-otp")
    public ApiResponse<Object> verifyOtp(@RequestBody VerifyOtpRequest request) {
        boolean valid = emailService.verifyRegisterOtp(request.getEmail(), request.getOtp()); // ✅ dùng đúng hàm
        if (!valid) {
            return ApiResponse.builder()
                    .code(400)
                    .message("OTP không hợp lệ hoặc đã hết hạn")
                    .build();
        }
        return ApiResponse.builder()
                .code(200)
                .message("Xác minh OTP thành công")
                .build();
    }

    @PostMapping("/register/confirm")
    public ApiResponse<Object> confirmRegister(@RequestBody RegisterRequest request) {
        if (!emailService.isVerified(request.getEmail())) {
            return ApiResponse.builder()
                    .code(400)
                    .message("Vui lòng xác minh OTP trước khi đăng ký")
                    .build();
        }

        RegisterReponse response = userService.register(request);
        return ApiResponse.builder()
                .code(200)
                .message("Đăng ký thành công")
                .data(response)
                .build();
    }


    @PostMapping("/login")
    public ApiResponse<Object> login(@RequestBody LoginRequest request) throws ParseException {
        LoginResponse loginResponse = userService.login(request);
        return ApiResponse.builder()
                .message(SUCCESS_MESSAGE)
                .code(SUCCESS)
                .data(loginResponse)
                .build();
    }

    @PostMapping("/change-password")
    public ApiResponse<Object> changePassword(@RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        userService.changePassword(username, request);
        
        return ApiResponse.builder()
                .message(PASSWORD_CHANGE_SUCESSFUL)
                .code(SUCCESS)
                .data(SUCCESS)
                .build();
    }

    @GetMapping("/{username}")
    public ApiResponse<Object> changePassword(@PathVariable String username) {
        return ApiResponse.builder()
                .message(PASSWORD_CHANGE_SUCESSFUL)
                .code(SUCCESS)
                .data(userService.getUserById(username))
                .build();
    }

    @GetMapping("/balance/{id}")
    public ApiResponse<Object> getBalance(@PathVariable Long id) {
        return ApiResponse.builder()
                .message(SUCCESS_MESSAGE)
                .code(SUCCESS)
                .data(userService.getBalance(id))
                .build();
    }


} 