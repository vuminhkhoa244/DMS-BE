package com.example.DocumentManagement.controller;

import com.example.DocumentManagement.dto.response.ApiResponse;
import com.example.DocumentManagement.dto.response.AuditLogResponse;
import com.example.DocumentManagement.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @RequestParam(value = "userId", required = false) UUID userId,
            @RequestParam(value = "action", required = false) String action,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<AuditLogResponse> logs;
        if (userId != null) {
            logs = auditLogService.getByUser(userId, pageable);
        } else if (action != null) {
            logs = auditLogService.getByAction(action, pageable);
        } else {
            logs = auditLogService.getAuditLogs(pageable);
        }
        return ResponseEntity.ok(ApiResponse.ok("Audit logs retrieved", logs));
    }
}
