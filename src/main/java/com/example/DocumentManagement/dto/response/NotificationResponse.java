package com.example.DocumentManagement.dto.response;

import com.example.DocumentManagement.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private String message;
    private Long documentId;
    private boolean read;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getMessage(),
                n.getDocument() != null ? n.getDocument().getId() : null,
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
