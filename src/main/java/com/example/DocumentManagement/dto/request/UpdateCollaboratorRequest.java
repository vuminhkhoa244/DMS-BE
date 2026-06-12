package com.example.DocumentManagement.dto.request;

import com.example.DocumentManagement.entity.CollaboratorPermission;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCollaboratorRequest {

    @NotNull
    private CollaboratorPermission permission;
}
