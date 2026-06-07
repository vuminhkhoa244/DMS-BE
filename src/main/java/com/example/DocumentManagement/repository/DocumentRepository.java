package com.example.DocumentManagement.repository;

import com.example.DocumentManagement.entity.Document;
import com.example.DocumentManagement.entity.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Page<Document> findByDeletedAtIsNull(Pageable pageable);

    Page<Document> findByCreatedByIdAndDeletedAtIsNull(java.util.UUID userId, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.deletedAt IS NULL " +
            "AND (:title IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
            "AND (:categoryId IS NULL OR d.category.id = :categoryId) " +
            "AND (:status IS NULL OR d.status = :status) " +
            "AND (:from IS NULL OR d.createdAt >= :from) " +
            "AND (:to IS NULL OR d.createdAt <= :to)")
    Page<Document> search(@Param("title") String title,
                          @Param("categoryId") Long categoryId,
                          @Param("status") DocumentStatus status,
                          @Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to,
                          Pageable pageable);

    @Query("SELECT d FROM Document d JOIN d.tags t WHERE d.deletedAt IS NULL AND t IN :tags")
    Page<Document> findByTagsIn(@Param("tags") java.util.List<String> tags, Pageable pageable);

    Page<Document> findByDeletedAtIsNotNull(Pageable pageable);
}
