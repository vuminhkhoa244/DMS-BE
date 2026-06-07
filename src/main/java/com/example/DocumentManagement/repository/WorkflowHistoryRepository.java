package com.example.DocumentManagement.repository;

import com.example.DocumentManagement.entity.WorkflowHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowHistoryRepository extends JpaRepository<WorkflowHistory, Long> {

    List<WorkflowHistory> findByDocumentIdOrderByCreatedAtDesc(Long documentId);
}
