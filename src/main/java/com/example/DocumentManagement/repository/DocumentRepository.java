package com.example.DocumentManagement.repository;

import com.example.DocumentManagement.entity.Document;
import com.example.DocumentManagement.entity.DocumentStatus;
import com.example.DocumentManagement.entity.DocumentVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {

    Page<Document> findByDeletedAtIsNull(Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.deletedAt IS NULL AND d.organization IS NULL " +
           "AND d.visibility = com.example.DocumentManagement.entity.DocumentVisibility.PUBLIC " +
           "AND d.status = com.example.DocumentManagement.entity.DocumentStatus.APPROVED")
    Page<Document> findPublicPersonal(Pageable pageable);

    Page<Document> findByCreatedByIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    // Personal workspace: files created by user with no organization
    Page<Document> findByCreatedByIdAndOrganizationIsNullAndDeletedAtIsNull(UUID userId, Pageable pageable);

    Page<Document> findByOrganizationIdAndDeletedAtIsNull(UUID organizationId, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.deletedAt IS NULL " +
           "AND d.organization.id = :orgId AND d.visibility IN :visibilities")
    Page<Document> findByOrgAndVisibilityIn(@Param("orgId") UUID orgId,
                                            @Param("visibilities") Collection<DocumentVisibility> visibilities,
                                            Pageable pageable);

    @Query("SELECT DISTINCT d FROM Document d JOIN d.tags t WHERE d.deletedAt IS NULL AND t IN :tags")
    Page<Document> findByTagsIn(@Param("tags") List<String> tags, Pageable pageable);

    Page<Document> findByDeletedAtIsNotNull(Pageable pageable);

    List<Document> findByOrganizationId(UUID organizationId);

    @Modifying
    @Query("UPDATE Document d SET d.organization = null WHERE d.organization.id = :orgId")
    void detachFromOrganization(@Param("orgId") UUID orgId);

    @Modifying
    @Query("UPDATE Document d SET d.deletedAt = CURRENT_TIMESTAMP WHERE d.createdBy.id = :userId AND d.deletedAt IS NULL")
    void softDeleteByOwnerId(@Param("userId") UUID userId);

    @Query("""
            SELECT d FROM Document d
            LEFT JOIN d.organization o
            WHERE d.deletedAt IS NULL AND (
                (d.createdBy.id = :userId)
                OR (d.id IN :collaboratorDocIds)
                OR (o IS NOT NULL AND o.id IN :adminOrgIds)
                OR (o IS NULL AND d.visibility = com.example.DocumentManagement.entity.DocumentVisibility.PUBLIC
                    AND d.status = com.example.DocumentManagement.entity.DocumentStatus.APPROVED)
                OR (o IS NOT NULL AND o.id IN :memberOrgIds AND d.visibility IN :memberVisibilities
                    AND d.status IN :publishedStatuses)
                OR (o IS NOT NULL AND o.visibility = com.example.DocumentManagement.entity.OrgVisibility.PUBLIC
                    AND d.visibility = com.example.DocumentManagement.entity.DocumentVisibility.ORG_PUBLIC
                    AND d.status IN :publishedStatuses)
            )
            """)
    Page<Document> findAccessible(@Param("userId") UUID userId,
                                  @Param("memberOrgIds") Collection<UUID> memberOrgIds,
                                  @Param("adminOrgIds") Collection<UUID> adminOrgIds,
                                  @Param("memberVisibilities") Collection<DocumentVisibility> memberVisibilities,
                                  @Param("collaboratorDocIds") Collection<Long> collaboratorDocIds,
                                  @Param("publishedStatuses") Collection<DocumentStatus> publishedStatuses,
                                  Pageable pageable);
}
