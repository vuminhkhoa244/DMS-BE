package com.example.DocumentManagement.dto.request;

import com.example.DocumentManagement.entity.OrgRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddMemberRequest {

    @NotBlank
    private String email;

    @NotNull
    private OrgRole orgRole = OrgRole.VIEWER;
}
