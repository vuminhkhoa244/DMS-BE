package com.example.DocumentManagement.dto.request;

import com.example.DocumentManagement.entity.OrgRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMemberRoleRequest {

    @NotNull
    private OrgRole orgRole;
}
