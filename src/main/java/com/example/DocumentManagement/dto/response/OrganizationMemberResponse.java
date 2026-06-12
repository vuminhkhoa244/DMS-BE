package com.example.DocumentManagement.dto.response;

import com.example.DocumentManagement.entity.OrganizationMember;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class OrganizationMemberResponse {

    private Long id;
    private UUID userId;
    private String email;
    private String fullName;
    private String orgRole;
    private LocalDateTime joinedAt;

    public static OrganizationMemberResponse from(OrganizationMember m) {
        return new OrganizationMemberResponse(
                m.getId(),
                m.getUser().getId(),
                m.getUser().getEmail(),
                m.getUser().getFullName(),
                m.getOrgRole().name(),
                m.getJoinedAt()
        );
    }
}
