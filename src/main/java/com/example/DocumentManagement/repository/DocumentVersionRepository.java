package com.example.DocumentManagement.repository;

import com.example.DocumentManagement.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

    List<DocumentVersion> findByDocumentIdOrderByVersionNumberDesc(Long documentId);

    Optional<DocumentVersion> findByDocumentIdAndVersionNumber(Long documentId, int versionNumber);

    Optional<DocumentVersion> findTopByDocumentIdOrderByVersionNumberDesc(Long documentId);
}
