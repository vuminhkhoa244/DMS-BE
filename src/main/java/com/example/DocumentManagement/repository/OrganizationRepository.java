package com.example.DocumentManagement.repository;

import com.example.DocumentManagement.entity.Organization;
import com.example.DocumentManagement.entity.OrgVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByOwnerId(UUID ownerId);

    Page<Organization> findByVisibility(OrgVisibility visibility, Pageable pageable);

    @Query("SELECT o FROM Organization o WHERE o.id IN " +
           "(SELECT m.organization.id FROM OrganizationMember m WHERE m.user.id = :userId)")
    Page<Organization> findAllByMemberUserId(@Param("userId") UUID userId, Pageable pageable);
}
