package com.example.DocumentManagement.repository;

import com.example.DocumentManagement.entity.WorkflowHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowHistoryRepository extends JpaRepository<WorkflowHistory, Long> {

    List<WorkflowHistory> findByDocumentIdOrderByCreatedAtDesc(Long documentId);

    @Modifying
    @Query("UPDATE WorkflowHistory w SET w.performedBy = null WHERE w.performedBy.id = :userId")
    void nullifyPerformedBy(@Param("userId") UUID userId);
}
