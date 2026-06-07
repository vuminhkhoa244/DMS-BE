package com.example.DocumentManagement.controller;

import com.example.DocumentManagement.dto.request.ChangePasswordRequest;
import com.example.DocumentManagement.dto.request.CreateUserRequest;
import com.example.DocumentManagement.dto.request.UpdateUserRequest;
import com.example.DocumentManagement.dto.response.ApiResponse;
import com.example.DocumentManagement.dto.response.UserResponse;
import com.example.DocumentManagement.entity.User;
import com.example.DocumentManagement.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request,
                                                                 @AuthenticationPrincipal User admin) {
        UserResponse response = userService.createUser(request, admin);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User created successfully", response));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@AuthenticationPrincipal User user) {
        UserResponse response = userService.getProfile(user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Profile retrieved", response));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@AuthenticationPrincipal User user,
                                                                    @RequestBody UpdateUserRequest request) {
        UserResponse response = userService.updateProfile(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", response));
    }

    @PutMapping("/profile/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@AuthenticationPrincipal User user,
                                                             @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed", null));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.ok("Users retrieved", users));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateRole(@PathVariable UUID id,
                                                                 @RequestParam String role,
                                                                 @AuthenticationPrincipal User admin) {
        UserResponse response = userService.updateRole(id, role, admin);
        return ResponseEntity.ok(ApiResponse.ok("Role updated", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id,
                                                         @AuthenticationPrincipal User admin) {
        userService.deleteUser(id, admin);
        return ResponseEntity.ok(ApiResponse.ok("User deleted", null));
    }
}
