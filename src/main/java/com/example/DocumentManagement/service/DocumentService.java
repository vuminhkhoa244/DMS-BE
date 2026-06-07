package com.example.DocumentManagement.service;

import com.example.DocumentManagement.dto.request.DocumentRequest;
import com.example.DocumentManagement.dto.response.DocumentResponse;
import com.example.DocumentManagement.dto.response.DocumentVersionResponse;
import com.example.DocumentManagement.entity.*;
import com.example.DocumentManagement.exception.BadRequestException;
import com.example.DocumentManagement.exception.ResourceNotFoundException;
import com.example.DocumentManagement.repository.CategoryRepository;
import com.example.DocumentManagement.repository.DocumentRepository;
import com.example.DocumentManagement.repository.DocumentVersionRepository;
import com.example.DocumentManagement.repository.WorkflowHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final WorkflowHistoryRepository workflowHistoryRepository;
    private final CategoryRepository categoryRepository;
    private final CloudinaryService cloudinaryService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentVersionRepository versionRepository,
                           WorkflowHistoryRepository workflowHistoryRepository,
                           CategoryRepository categoryRepository,
                           CloudinaryService cloudinaryService,
                           AuditLogService auditLogService,
                           NotificationService notificationService) {
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.workflowHistoryRepository = workflowHistoryRepository;
        this.categoryRepository = categoryRepository;
        this.cloudinaryService = cloudinaryService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Transactional
    public DocumentResponse createDocument(DocumentRequest request, MultipartFile file, User user) throws IOException {
        Document document = new Document();
        document.setTitle(request.getTitle());
        document.setDescription(request.getDescription());
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            document.setCategory(category);
        }
        document.setCreatedBy(user);
        if (request.getTags() != null) {
            document.setTags(request.getTags());
        }
        documentRepository.save(document);

        // Upload file and create version 1
        Map<String, Object> uploadResult = cloudinaryService.upload(file);

        DocumentVersion version = new DocumentVersion();
        version.setDocument(document);
        version.setVersionNumber(1);
        version.setFileUrl((String) uploadResult.get("secure_url"));
        version.setCloudinaryPublicId((String) uploadResult.get("public_id"));
        version.setFileType(file.getContentType());
        version.setFileSize(file.getSize());
        version.setComment("Initial upload");
        version.setUploadedBy(user);
        versionRepository.save(version);

        document.getVersions().add(version);

        auditLogService.log(user, "CREATE_DOCUMENT", "Document", document.getId(),
                "Created document: " + document.getTitle());

        return DocumentResponse.from(document);
    }

    public Page<DocumentResponse> getDocuments(Pageable pageable) {
        return documentRepository.findByDeletedAtIsNull(pageable).map(DocumentResponse::from);
    }

    public Page<DocumentResponse> getMyDocuments(java.util.UUID userId, Pageable pageable) {
        return documentRepository.findByCreatedByIdAndDeletedAtIsNull(userId, pageable)
                .map(DocumentResponse::from);
    }

    public Page<DocumentResponse> search(String title, Long categoryId, DocumentStatus status,
                                          LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return documentRepository.search(title, categoryId, status, from, to, pageable)
                .map(DocumentResponse::from);
    }

    public Page<DocumentResponse> searchByTags(List<String> tags, Pageable pageable) {
        return documentRepository.findByTagsIn(tags, pageable).map(DocumentResponse::from);
    }

    public DocumentResponse getDocument(Long documentId) {
        Document document = findActiveDocument(documentId);
        return DocumentResponse.from(document);
    }

    @Transactional
    public DocumentResponse updateDocument(Long documentId, DocumentRequest request, User user) {
        Document document = findActiveDocument(documentId);
        checkOwnerOrManager(document, user);

        document.setTitle(request.getTitle());
        if (request.getDescription() != null) {
            document.setDescription(request.getDescription());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            document.setCategory(category);
        }
        if (request.getTags() != null) {
            document.setTags(request.getTags());
        }

        documentRepository.save(document);

        auditLogService.log(user, "UPDATE_DOCUMENT", "Document", document.getId(),
                "Updated metadata for: " + document.getTitle());

        return DocumentResponse.from(document);
    }

    @Transactional
    public void softDeleteDocument(Long documentId, User user) {
        Document document = findActiveDocument(documentId);
        checkOwnerOrManager(document, user);

        document.setDeletedAt(LocalDateTime.now());
        documentRepository.save(document);

        auditLogService.log(user, "DELETE_DOCUMENT", "Document", document.getId(),
                "Soft deleted document: " + document.getTitle());
    }

    // --- Versioning ---

    @Transactional
    public DocumentVersionResponse uploadNewVersion(Long documentId, MultipartFile file,
                                                     String comment, User user) throws IOException {
        Document document = findActiveDocument(documentId);

        Map<String, Object> uploadResult = cloudinaryService.upload(file);

        int nextVersion = versionRepository.findTopByDocumentIdOrderByVersionNumberDesc(documentId)
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        DocumentVersion version = new DocumentVersion();
        version.setDocument(document);
        version.setVersionNumber(nextVersion);
        version.setFileUrl((String) uploadResult.get("secure_url"));
        version.setCloudinaryPublicId((String) uploadResult.get("public_id"));
        version.setFileType(file.getContentType());
        version.setFileSize(file.getSize());
        version.setComment(comment);
        version.setUploadedBy(user);
        versionRepository.save(version);

        auditLogService.log(user, "UPLOAD_VERSION", "Document", document.getId(),
                "Uploaded version " + nextVersion);

        // Notify document owner
        if (!document.getCreatedBy().getId().equals(user.getId())) {
            notificationService.notify(document.getCreatedBy(),
                    user.getFullName() + " uploaded a new version (v" + nextVersion + ") of \"" + document.getTitle() + "\"",
                    document);
        }

        return DocumentVersionResponse.from(version);
    }

    public List<DocumentVersionResponse> getVersionHistory(Long documentId) {
        findActiveDocument(documentId);
        return versionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId).stream()
                .map(DocumentVersionResponse::from)
                .toList();
    }

    public DocumentVersion getVersion(Long documentId, int versionNumber) {
        return versionRepository.findByDocumentIdAndVersionNumber(documentId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Version", "number", versionNumber));
    }

    @Transactional
    public DocumentVersionResponse rollback(Long documentId, int targetVersion, String reason, User user) throws IOException {
        Document document = findActiveDocument(documentId);

        DocumentVersion oldVersion = versionRepository.findByDocumentIdAndVersionNumber(documentId, targetVersion)
                .orElseThrow(() -> new ResourceNotFoundException("Version", "number", targetVersion));

        int nextVersion = versionRepository.findTopByDocumentIdOrderByVersionNumberDesc(documentId)
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        // Rollback creates a new version (non-destructive)
        DocumentVersion newVersion = new DocumentVersion();
        newVersion.setDocument(document);
        newVersion.setVersionNumber(nextVersion);
        newVersion.setFileUrl(oldVersion.getFileUrl());
        newVersion.setCloudinaryPublicId(oldVersion.getCloudinaryPublicId());
        newVersion.setFileType(oldVersion.getFileType());
        newVersion.setFileSize(oldVersion.getFileSize());
        newVersion.setComment("Rollback to v" + targetVersion + ". Reason: " + reason);
        newVersion.setUploadedBy(user);
        versionRepository.save(newVersion);

        auditLogService.log(user, "ROLLBACK_VERSION", "Document", document.getId(),
                "Rolled back to version " + targetVersion + ". Reason: " + reason);

        return DocumentVersionResponse.from(newVersion);
    }

    // --- Workflow ---

    @Transactional
    public DocumentResponse submitForReview(Long documentId, String comment, User user) {
        Document document = findActiveDocument(documentId);
        if (!document.getCreatedBy().getId().equals(user.getId())) {
            throw new BadRequestException("Only document owner can submit for review");
        }
        if (document.getStatus() != DocumentStatus.DRAFT && document.getStatus() != DocumentStatus.REJECTED) {
            throw new BadRequestException("Document can only be submitted from DRAFT or REJECTED status");
        }

        DocumentStatus oldStatus = document.getStatus();
        document.setStatus(DocumentStatus.PENDING_REVIEW);
        documentRepository.save(document);

        saveWorkflowHistory(document, oldStatus, DocumentStatus.PENDING_REVIEW, comment, user);

        auditLogService.log(user, "SUBMIT_FOR_REVIEW", "Document", document.getId(),
                "Submitted for review: " + document.getTitle());

        return DocumentResponse.from(document);
    }

    @Transactional
    public DocumentResponse approveDocument(Long documentId, String comment, User user) {
        Document document = findActiveDocument(documentId);
        if (document.getStatus() != DocumentStatus.PENDING_REVIEW) {
            throw new BadRequestException("Document must be in PENDING_REVIEW status to approve");
        }
        if (document.getCreatedBy().getId().equals(user.getId())) {
            throw new BadRequestException("Cannot approve own document");
        }

        document.setStatus(DocumentStatus.APPROVED);
        documentRepository.save(document);

        saveWorkflowHistory(document, DocumentStatus.PENDING_REVIEW, DocumentStatus.APPROVED, comment, user);

        notificationService.notify(document.getCreatedBy(),
                "Your document \"" + document.getTitle() + "\" has been approved by " + user.getFullName(),
                document);

        auditLogService.log(user, "APPROVE_DOCUMENT", "Document", document.getId(),
                "Approved: " + document.getTitle());

        return DocumentResponse.from(document);
    }

    @Transactional
    public DocumentResponse rejectDocument(Long documentId, String comment, User user) {
        Document document = findActiveDocument(documentId);
        if (document.getStatus() != DocumentStatus.PENDING_REVIEW) {
            throw new BadRequestException("Document must be in PENDING_REVIEW status to reject");
        }

        document.setStatus(DocumentStatus.REJECTED);
        documentRepository.save(document);

        saveWorkflowHistory(document, DocumentStatus.PENDING_REVIEW, DocumentStatus.REJECTED, comment, user);

        notificationService.notify(document.getCreatedBy(),
                "Your document \"" + document.getTitle() + "\" has been rejected by " + user.getFullName()
                        + (comment != null ? ". Reason: " + comment : ""),
                document);

        auditLogService.log(user, "REJECT_DOCUMENT", "Document", document.getId(),
                "Rejected: " + document.getTitle());

        return DocumentResponse.from(document);
    }

    @Transactional
    public DocumentResponse archiveDocument(Long documentId, String comment, User user) {
        Document document = findActiveDocument(documentId);
        if (document.getStatus() != DocumentStatus.APPROVED) {
            throw new BadRequestException("Only APPROVED documents can be archived");
        }

        document.setStatus(DocumentStatus.ARCHIVED);
        documentRepository.save(document);

        saveWorkflowHistory(document, DocumentStatus.APPROVED, DocumentStatus.ARCHIVED, comment, user);

        auditLogService.log(user, "ARCHIVE_DOCUMENT", "Document", document.getId(),
                "Archived: " + document.getTitle());

        return DocumentResponse.from(document);
    }

    public List<com.example.DocumentManagement.dto.response.WorkflowHistoryResponse> getWorkflowHistory(Long documentId) {
        findActiveDocument(documentId);
        return workflowHistoryRepository.findByDocumentIdOrderByCreatedAtDesc(documentId).stream()
                .map(com.example.DocumentManagement.dto.response.WorkflowHistoryResponse::from)
                .toList();
    }

    // --- Helpers ---

    private Document findActiveDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));
        if (document.isDeleted()) {
            throw new ResourceNotFoundException("Document", "id", documentId);
        }
        return document;
    }

    private void checkOwnerOrManager(Document document, User user) {
        if (!document.getCreatedBy().getId().equals(user.getId())
                && user.getRole() != Role.MANAGER && user.getRole() != Role.ADMIN) {
            throw new BadRequestException("You don't have permission to modify this document");
        }
    }

    private void saveWorkflowHistory(Document document, DocumentStatus from, DocumentStatus to,
                                      String comment, User user) {
        WorkflowHistory history = new WorkflowHistory();
        history.setDocument(document);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setComment(comment);
        history.setPerformedBy(user);
        workflowHistoryRepository.save(history);
    }
}
