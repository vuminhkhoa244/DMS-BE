package com.example.DocumentManagement.dto.response;

import com.example.DocumentManagement.entity.Document;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class DocumentResponse {

    private Long id;
    private String title;
    private String description;
    private Long categoryId;
    private String categoryName;
    private String status;
    private String visibility;
    private UUID organizationId;
    private String organizationName;
    private List<String> tags;
    private UUID createdById;
    private String createdByName;
    private int latestVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DocumentResponse from(Document doc) {
        return new DocumentResponse(
                doc.getId(),
                doc.getTitle(),
                doc.getDescription(),
                doc.getCategory() != null ? doc.getCategory().getId() : null,
                doc.getCategory() != null ? doc.getCategory().getName() : null,
                doc.getStatus().name(),
                doc.getVisibility().name(),
                doc.getOrganization() != null ? doc.getOrganization().getId() : null,
                doc.getOrganization() != null ? doc.getOrganization().getName() : null,
                doc.getTags(),
                doc.getCreatedBy().getId(),
                doc.getCreatedBy().getFullName(),
                doc.getLatestVersion(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
