package com.example.DocumentManagement.controller;

import com.example.DocumentManagement.dto.request.LoginRequest;
import com.example.DocumentManagement.dto.request.RegisterRequest;
import com.example.DocumentManagement.dto.response.ApiResponse;
import com.example.DocumentManagement.dto.response.AuthResponse;
import com.example.DocumentManagement.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse authResponse = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User registered successfully", authResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = userService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", authResponse));
    }

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<Map<String, String>>> getRoles() {
        Map<String, String> roles = Map.of(
                "ADMIN", "Quản trị viên - Toàn quyền hệ thống",
                "MANAGER", "Quản lý - Duyệt/từ chối/lưu trữ tài liệu, quản lý category",
                "STAFF", "Nhân viên - Upload, xem, chỉnh sửa tài liệu của mình"
        );
        return ResponseEntity.ok(ApiResponse.ok("Available roles", roles));
    }
}
