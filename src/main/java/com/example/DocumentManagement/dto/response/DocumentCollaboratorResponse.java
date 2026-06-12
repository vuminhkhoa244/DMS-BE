package com.example.DocumentManagement.dto.response;

import com.example.DocumentManagement.entity.DocumentCollaborator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class DocumentCollaboratorResponse {

    private Long id;
    private UUID userId;
    private String email;
    private String fullName;
    private String permission;
    private UUID addedById;
    private String addedByName;
    private LocalDateTime addedAt;

    public static DocumentCollaboratorResponse from(DocumentCollaborator c) {
        return new DocumentCollaboratorResponse(
                c.getId(),
                c.getUser().getId(),
                c.getUser().getEmail(),
                c.getUser().getFullName(),
                c.getPermission().name(),
                c.getAddedBy().getId(),
                c.getAddedBy().getFullName(),
                c.getAddedAt()
        );
    }
}
