package com.example.DocumentManagement.repository;

import com.example.DocumentManagement.entity.OrgRole;
import com.example.DocumentManagement.entity.OrganizationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {

    Optional<OrganizationMember> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    boolean existsByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    List<OrganizationMember> findByOrganizationId(UUID organizationId);

    List<OrganizationMember> findByUserId(UUID userId);

    @Query("SELECT m.organization.id FROM OrganizationMember m WHERE m.user.id = :userId")
    List<UUID> findOrgIdsByUserId(@Param("userId") UUID userId);

    @Query("SELECT m.organization.id FROM OrganizationMember m WHERE m.user.id = :userId AND m.orgRole IN :roles")
    List<UUID> findOrgIdsByUserIdAndRoleIn(@Param("userId") UUID userId, @Param("roles") Collection<OrgRole> roles);

    void deleteByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    void deleteByOrganizationId(UUID organizationId);

    void deleteByUserId(UUID userId);
}
