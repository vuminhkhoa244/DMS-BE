package com.example.DocumentManagement.controller;

import com.example.DocumentManagement.dto.response.ApiResponse;
import com.example.DocumentManagement.dto.response.NotificationResponse;
import com.example.DocumentManagement.entity.User;
import com.example.DocumentManagement.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNotifications(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<NotificationResponse> notifications = notificationService.getNotifications(user.getId(), pageable);
        long unreadCount = notificationService.getUnreadCount(user.getId());

        Map<String, Object> data = Map.of(
                "notifications", notifications,
                "unreadCount", unreadCount
        );
        return ResponseEntity.ok(ApiResponse.ok("Notifications retrieved", data));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id,
                                                         @AuthenticationPrincipal User user) {
        notificationService.markAsRead(id, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Marked as read", null));
    }
}
