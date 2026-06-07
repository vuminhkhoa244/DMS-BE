package com.example.DocumentManagement.dto.response;

import com.example.DocumentManagement.entity.DocumentVersion;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class DocumentVersionResponse {

    private Long id;
    private int versionNumber;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private String comment;
    private UUID uploadedById;
    private String uploadedByName;
    private LocalDateTime createdAt;

    public static DocumentVersionResponse from(DocumentVersion v) {
        return new DocumentVersionResponse(
                v.getId(),
                v.getVersionNumber(),
                v.getFileUrl(),
                v.getFileType(),
                v.getFileSize(),
                v.getComment(),
                v.getUploadedBy().getId(),
                v.getUploadedBy().getFullName(),
                v.getCreatedAt()
        );
    }
}
