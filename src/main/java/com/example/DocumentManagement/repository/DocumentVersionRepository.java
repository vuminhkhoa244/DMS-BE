package com.example.DocumentManagement.repository;

import com.example.DocumentManagement.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

    List<DocumentVersion> findByDocumentIdOrderByVersionNumberDesc(Long documentId);

    Optional<DocumentVersion> findByDocumentIdAndVersionNumber(Long documentId, int versionNumber);

    Optional<DocumentVersion> findTopByDocumentIdOrderByVersionNumberDesc(Long documentId);

    @Modifying
    @Query("UPDATE DocumentVersion v SET v.uploadedBy = null WHERE v.uploadedBy.id = :userId")
    void nullifyUploadedBy(@Param("userId") UUID userId);
}
