package com.example.DocumentManagement.dto.request;

import com.example.DocumentManagement.entity.CollaboratorPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddCollaboratorRequest {

    @NotBlank
    private String email;

    @NotNull
    private CollaboratorPermission permission = CollaboratorPermission.READ;
}
