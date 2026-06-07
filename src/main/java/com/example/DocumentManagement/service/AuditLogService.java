package com.example.DocumentManagement.service;

import com.example.DocumentManagement.dto.response.AuditLogResponse;
import com.example.DocumentManagement.entity.AuditLog;
import com.example.DocumentManagement.entity.User;
import com.example.DocumentManagement.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(User user, String action, String entityType, Long entityId, String details) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    public Page<AuditLogResponse> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable).map(AuditLogResponse::from);
    }

    public Page<AuditLogResponse> getByUser(UUID userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable).map(AuditLogResponse::from);
    }

    public Page<AuditLogResponse> getByAction(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable).map(AuditLogResponse::from);
    }
}
