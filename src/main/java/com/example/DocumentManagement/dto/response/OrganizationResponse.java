package com.example.DocumentManagement.dto.response;

import com.example.DocumentManagement.entity.Organization;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class OrganizationResponse {

    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String visibility;
    private UUID ownerId;
    private String ownerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrganizationResponse from(Organization org) {
        return new OrganizationResponse(
                org.getId(),
                org.getName(),
                org.getSlug(),
                org.getDescription(),
                org.getVisibility().name(),
                org.getOwner().getId(),
                org.getOwner().getFullName(),
                org.getCreatedAt(),
                org.getUpdatedAt()
        );
    }
}
