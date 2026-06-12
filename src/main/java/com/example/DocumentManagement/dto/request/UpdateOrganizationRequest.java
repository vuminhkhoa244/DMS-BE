package com.example.DocumentManagement.dto.request;

import com.example.DocumentManagement.entity.OrgVisibility;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateOrganizationRequest {

    @Size(max = 100)
    private String name;

    @Size(max = 2000)
    private String description;

    private OrgVisibility visibility;
}
