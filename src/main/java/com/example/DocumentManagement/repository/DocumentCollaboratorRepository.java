package com.example.DocumentManagement.repository;

import com.example.DocumentManagement.entity.DocumentCollaborator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentCollaboratorRepository extends JpaRepository<DocumentCollaborator, Long> {

    Optional<DocumentCollaborator> findByDocumentIdAndUserId(Long documentId, UUID userId);

    List<DocumentCollaborator> findByDocumentId(Long documentId);

    void deleteByDocumentIdAndUserId(Long documentId, UUID userId);

    @Query("SELECT c.document.id FROM DocumentCollaborator c WHERE c.user.id = :userId")
    List<Long> findDocumentIdsByUserId(@Param("userId") UUID userId);

    void deleteByUserId(UUID userId);
}
