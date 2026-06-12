package com.example.DocumentManagement.dto.request;

import com.example.DocumentManagement.entity.OrgVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrganizationRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "^[a-z0-9-]+$", message = "slug must be lowercase letters, digits, or dashes")
    private String slug;

    @Size(max = 2000)
    private String description;

    private OrgVisibility visibility = OrgVisibility.PRIVATE;
}
