package com.example.DocumentManagement.dto.request;

import com.example.DocumentManagement.entity.DocumentVisibility;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class DocumentRequest {

    @NotBlank
    private String title;

    private String description;

    private Long categoryId;

    private List<String> tags;

    /**
     * Target organization. Null = personal (private) space of the uploader.
     */
    private UUID organizationId;

    /**
     * Defaults:
     *  - if organizationId == null → PRIVATE
     *  - if organizationId != null → ORG_INTERNAL
     */
    private DocumentVisibility visibility;
}
