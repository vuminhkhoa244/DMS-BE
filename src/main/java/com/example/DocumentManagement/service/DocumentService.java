package com.example.DocumentManagement.service;

import com.example.DocumentManagement.dto.request.AddCollaboratorRequest;
import com.example.DocumentManagement.dto.request.DocumentRequest;
import com.example.DocumentManagement.dto.request.UpdateCollaboratorRequest;
import com.example.DocumentManagement.dto.response.DocumentCollaboratorResponse;
import com.example.DocumentManagement.dto.response.DocumentResponse;
import com.example.DocumentManagement.dto.response.DocumentVersionResponse;
import com.example.DocumentManagement.entity.*;
import com.example.DocumentManagement.exception.BadRequestException;
import com.example.DocumentManagement.exception.ResourceNotFoundException;
import com.example.DocumentManagement.repository.CategoryRepository;
import com.example.DocumentManagement.repository.DocumentCollaboratorRepository;
import com.example.DocumentManagement.repository.DocumentRepository;
import com.example.DocumentManagement.repository.DocumentVersionRepository;
import com.example.DocumentManagement.repository.OrganizationMemberRepository;
import com.example.DocumentManagement.repository.OrganizationRepository;
import com.example.DocumentManagement.repository.UserRepository;
import com.example.DocumentManagement.repository.WorkflowHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final WorkflowHistoryRepository workflowHistoryRepository;
    private final CategoryRepository categoryRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final DocumentCollaboratorRepository collaboratorRepository;
    private final UserRepository userRepository;
    private final MinioService minioService;
    private final DocumentAccessService accessService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentVersionRepository versionRepository,
                           WorkflowHistoryRepository workflowHistoryRepository,
                           CategoryRepository categoryRepository,
                           OrganizationRepository organizationRepository,
                           OrganizationMemberRepository memberRepository,
                           DocumentCollaboratorRepository collaboratorRepository,
                           UserRepository userRepository,
                           MinioService minioService,
                           DocumentAccessService accessService,
                           AuditLogService auditLogService,
                           NotificationService notificationService) {
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.workflowHistoryRepository = workflowHistoryRepository;
        this.categoryRepository = categoryRepository;
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.userRepository = userRepository;
        this.minioService = minioService;
        this.accessService = accessService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    // --- CRUD ---

    @Transactional
    public DocumentResponse createDocument(DocumentRequest request, MultipartFile file, User user) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        Organization org = null;
        if (request.getOrganizationId() != null) {
            org = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", request.getOrganizationId()));
            accessService.requireCanUpload(user, org.getId());
        }

        DocumentVisibility visibility = request.getVisibility();
        if (visibility == null) {
            visibility = (org == null) ? DocumentVisibility.PRIVATE : DocumentVisibility.ORG_INTERNAL;
        }
        validateVisibility(org, visibility);

        Document document = new Document();
        document.setTitle(request.getTitle());
        document.setDescription(request.getDescription());
        document.setCreatedBy(user);
        document.setOrganization(org);
        document.setVisibility(visibility);
        if (request.getCategoryId() != null) {
            document.setCategory(categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId())));
        }
        if (request.getTags() != null) document.setTags(request.getTags());
        documentRepository.save(document);

        // Build namespaced object key and upload
        String namespace = (org == null) ? "personal" : "orgs";
        String namespaceId = (org == null) ? user.getId().toString() : org.getId().toString();
        String objectKey = minioService.buildKey(namespace, namespaceId, file.getOriginalFilename());
        minioService.upload(file, objectKey);

        DocumentVersion version = new DocumentVersion();
        version.setDocument(document);
        version.setVersionNumber(1);
        version.setObjectKey(objectKey);
        version.setFileName(file.getOriginalFilename());
        version.setFileType(file.getContentType());
        version.setFileSize(file.getSize());
        version.setComment("Initial upload");
        version.setUploadedBy(user);
        versionRepository.save(version);

        document.setLatestObjectKey(objectKey);
        document.setLatestVersion(1);
        documentRepository.save(document);

        auditLogService.log(user, "CREATE_DOCUMENT", "Document", document.getId(),
                "Created document: " + document.getTitle());

        return DocumentResponse.from(document);
    }

    // --- Listing ---

    /** All documents this user can see (personal + all accessible orgs + collaborator shares). System ADMIN sees everything. */
    public Page<DocumentResponse> getAccessibleDocuments(User user, Pageable pageable) {
        if (user.getRole() == Role.ADMIN) {
            return documentRepository.findByDeletedAtIsNull(pageable).map(DocumentResponse::from);
        }
        List<UUID> memberOrgIds = memberRepository.findOrgIdsByUserId(user.getId());
        if (memberOrgIds.isEmpty()) memberOrgIds = List.of(new UUID(0L, 0L));

        List<UUID> adminOrgIds = memberRepository.findOrgIdsByUserIdAndRoleIn(
                user.getId(), List.of(OrgRole.ADMIN, OrgRole.OWNER));
        if (adminOrgIds.isEmpty()) adminOrgIds = List.of(new UUID(0L, 0L));

        List<Long> collaboratorDocIds = collaboratorRepository.findDocumentIdsByUserId(user.getId());
        if (collaboratorDocIds.isEmpty()) collaboratorDocIds = List.of(-1L);

        Set<DocumentVisibility> memberVisibilities =
                Set.of(DocumentVisibility.ORG_INTERNAL, DocumentVisibility.ORG_PUBLIC);
        Set<DocumentStatus> publishedStatuses =
                Set.of(DocumentStatus.APPROVED, DocumentStatus.ARCHIVED);
        return documentRepository
                .findAccessible(user.getId(), memberOrgIds, adminOrgIds,
                        memberVisibilities, collaboratorDocIds, publishedStatuses, pageable)
                .map(DocumentResponse::from);
    }

    /** Strictly the user's personal (no-org) documents. */
    public Page<DocumentResponse> getMyPersonalDocuments(User user, Pageable pageable) {
        return documentRepository
                .findByCreatedByIdAndOrganizationIsNullAndDeletedAtIsNull(user.getId(), pageable)
                .map(DocumentResponse::from);
    }

    /** Public personal files — discoverable by any authenticated user. */
    public Page<DocumentResponse> getPublicPersonalDocuments(Pageable pageable) {
        return documentRepository.findPublicPersonal(pageable).map(DocumentResponse::from);
    }

    public Page<DocumentResponse> getMyDocuments(UUID userId, Pageable pageable) {
        return documentRepository.findByCreatedByIdAndDeletedAtIsNull(userId, pageable)
                .map(DocumentResponse::from);
    }

    public Page<DocumentResponse> getOrgDocuments(UUID orgId, User user, Pageable pageable) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));
        boolean isMember = memberRepository.existsByOrganizationIdAndUserId(orgId, user.getId());
        boolean isSystemAdmin = user.getRole() == Role.ADMIN;

        Set<DocumentVisibility> visibilities;
        if (isSystemAdmin || isMember) {
            visibilities = Set.of(DocumentVisibility.ORG_INTERNAL, DocumentVisibility.ORG_PUBLIC);
        } else if (org.getVisibility() == OrgVisibility.PUBLIC) {
            visibilities = Set.of(DocumentVisibility.ORG_PUBLIC);
        } else {
            throw new BadRequestException("You don't have access to this organization");
        }

        return documentRepository.findByOrgAndVisibilityIn(orgId, visibilities, pageable)
                .map(DocumentResponse::from);
    }

    public DocumentResponse getDocument(Long documentId, User user) {
        Document document = findActiveDocument(documentId);
        accessService.requireView(user, document);
        return DocumentResponse.from(document);
    }

    @Transactional
    public DocumentResponse updateDocument(Long documentId, DocumentRequest request, User user) {
        Document document = findActiveDocument(documentId);
        accessService.requireEdit(user, document);

        document.setTitle(request.getTitle());
        if (request.getDescription() != null) document.setDescription(request.getDescription());
        if (request.getCategoryId() != null) {
            document.setCategory(categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId())));
        }
        if (request.getTags() != null) document.setTags(request.getTags());
        if (request.getVisibility() != null) {
            validateVisibility(document.getOrganization(), request.getVisibility());
            document.setVisibility(request.getVisibility());
        }

        documentRepository.save(document);
        auditLogService.log(user, "UPDATE_DOCUMENT", "Document", document.getId(),
                "Updated metadata for: " + document.getTitle());
        return DocumentResponse.from(document);
    }

    @Transactional
    public void softDeleteDocument(Long documentId, User user) {
        Document document = findActiveDocument(documentId);
        accessService.requireDelete(user, document);

        document.setDeletedAt(LocalDateTime.now());
        documentRepository.save(document);

        auditLogService.log(user, "DELETE_DOCUMENT", "Document", document.getId(),
                "Soft deleted document: " + document.getTitle());
    }

    // --- Versioning ---

    @Transactional
    public DocumentVersionResponse uploadNewVersion(Long documentId, MultipartFile file,
                                                     String comment, User user) {
        Document document = findActiveDocument(documentId);
        if (document.getStatus() == DocumentStatus.ARCHIVED) {
            throw new BadRequestException("Cannot upload new version to an archived document");
        }
        accessService.requireEdit(user, document);

        String namespace = (document.getOrganization() == null) ? "personal" : "orgs";
        String namespaceId = (document.getOrganization() == null)
                ? document.getCreatedBy().getId().toString()
                : document.getOrganization().getId().toString();
        String objectKey = minioService.buildKey(namespace, namespaceId, file.getOriginalFilename());
        minioService.upload(file, objectKey);

        int nextVersion = versionRepository.findTopByDocumentIdOrderByVersionNumberDesc(documentId)
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        DocumentVersion version = new DocumentVersion();
        version.setDocument(document);
        version.setVersionNumber(nextVersion);
        version.setObjectKey(objectKey);
        version.setFileName(file.getOriginalFilename());
        version.setFileType(file.getContentType());
        version.setFileSize(file.getSize());
        version.setComment(comment);
        version.setUploadedBy(user);
        versionRepository.save(version);

        document.setLatestObjectKey(objectKey);
        document.setLatestVersion(nextVersion);
        // New content resets APPROVED back to DRAFT — needs re-review.
        if (document.getStatus() == DocumentStatus.APPROVED) {
            document.setStatus(DocumentStatus.DRAFT);
        }
        documentRepository.save(document);

        auditLogService.log(user, "UPLOAD_VERSION", "Document", document.getId(),
                "Uploaded version " + nextVersion);

        if (!document.getCreatedBy().getId().equals(user.getId())) {
            notificationService.notify(document.getCreatedBy(),
                    user.getFullName() + " uploaded a new version (v" + nextVersion + ") of \""
                            + document.getTitle() + "\"", document);
        }

        return DocumentVersionResponse.from(version);
    }

    public List<DocumentVersionResponse> getVersionHistory(Long documentId, User user) {
        Document doc = findActiveDocument(documentId);
        accessService.requireView(user, doc);
        return versionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId).stream()
                .map(DocumentVersionResponse::from)
                .toList();
    }

    public String getDownloadUrl(Long documentId, User user) {
        Document document = findActiveDocument(documentId);
        accessService.requireView(user, document);

        if (document.getLatestObjectKey() == null || document.getLatestObjectKey().isBlank()) {
            throw new ResourceNotFoundException("Document", "file", documentId);
        }
        auditLogService.log(user, "DOWNLOAD", "Document", documentId,
                "Downloaded: " + document.getTitle());
        return minioService.presignedGetUrl(document.getLatestObjectKey());
    }

    public String getVersionDownloadUrl(Long documentId, int versionNumber, User user) {
        Document document = findActiveDocument(documentId);
        accessService.requireView(user, document);

        DocumentVersion version = versionRepository.findByDocumentIdAndVersionNumber(documentId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Version", "number", versionNumber));
        auditLogService.log(user, "DOWNLOAD_VERSION", "Document", documentId,
                "Downloaded v" + versionNumber + " of: " + document.getTitle());
        return minioService.presignedGetUrl(version.getObjectKey());
    }

    public DocumentVersion getVersion(Long documentId, int versionNumber) {
        return versionRepository.findByDocumentIdAndVersionNumber(documentId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Version", "number", versionNumber));
    }

    @Transactional
    public DocumentVersionResponse rollback(Long documentId, int targetVersion, String reason, User user) {
        Document document = findActiveDocument(documentId);
        accessService.requireEdit(user, document);

        DocumentVersion oldVersion = versionRepository.findByDocumentIdAndVersionNumber(documentId, targetVersion)
                .orElseThrow(() -> new ResourceNotFoundException("Version", "number", targetVersion));

        int nextVersion = versionRepository.findTopByDocumentIdOrderByVersionNumberDesc(documentId)
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        DocumentVersion newVersion = new DocumentVersion();
        newVersion.setDocument(document);
        newVersion.setVersionNumber(nextVersion);
        newVersion.setObjectKey(oldVersion.getObjectKey());
        newVersion.setFileName(oldVersion.getFileName());
        newVersion.setFileType(oldVersion.getFileType());
        newVersion.setFileSize(oldVersion.getFileSize());
        newVersion.setComment("Rollback to v" + targetVersion + (reason != null ? ". Reason: " + reason : ""));
        newVersion.setUploadedBy(user);
        versionRepository.save(newVersion);

        document.setLatestObjectKey(newVersion.getObjectKey());
        document.setLatestVersion(nextVersion);
        documentRepository.save(document);

        auditLogService.log(user, "ROLLBACK_VERSION", "Document", document.getId(),
                "Rolled back to version " + targetVersion);

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
        accessService.requireEdit(user, document); // approver must have edit rights

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
        accessService.requireEdit(user, document);

        if (document.getStatus() != DocumentStatus.PENDING_REVIEW) {
            throw new BadRequestException("Document must be in PENDING_REVIEW status to reject");
        }

        document.setStatus(DocumentStatus.REJECTED);
        documentRepository.save(document);

        saveWorkflowHistory(document, DocumentStatus.PENDING_REVIEW, DocumentStatus.REJECTED, comment, user);
        notificationService.notify(document.getCreatedBy(),
                "Your document \"" + document.getTitle() + "\" has been rejected by " + user.getFullName()
                        + (comment != null ? ". Reason: " + comment : ""), document);
        auditLogService.log(user, "REJECT_DOCUMENT", "Document", document.getId(),
                "Rejected: " + document.getTitle());
        return DocumentResponse.from(document);
    }

    @Transactional
    public DocumentResponse archiveDocument(Long documentId, String comment, User user) {
        Document document = findActiveDocument(documentId);
        accessService.requireEdit(user, document);

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

    public List<com.example.DocumentManagement.dto.response.WorkflowHistoryResponse> getWorkflowHistory(Long documentId, User user) {
        Document doc = findActiveDocument(documentId);
        accessService.requireView(user, doc);
        return workflowHistoryRepository.findByDocumentIdOrderByCreatedAtDesc(documentId).stream()
                .map(com.example.DocumentManagement.dto.response.WorkflowHistoryResponse::from)
                .toList();
    }

    // --- Helpers ---

    // --- Collaborators ---

    public List<DocumentCollaboratorResponse> listCollaborators(Long documentId, User user) {
        Document doc = findActiveDocument(documentId);
        accessService.requireView(user, doc);
        return collaboratorRepository.findByDocumentId(documentId).stream()
                .map(DocumentCollaboratorResponse::from)
                .toList();
    }

    @Transactional
    public DocumentCollaboratorResponse addCollaborator(Long documentId, AddCollaboratorRequest request, User actor) {
        Document doc = findActiveDocument(documentId);
        // Only users who can edit the doc can share it
        accessService.requireEdit(actor, doc);

        User target = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));
        if (target.getId().equals(doc.getCreatedBy().getId())) {
            throw new BadRequestException("Cannot add the document owner as a collaborator");
        }
        if (collaboratorRepository.findByDocumentIdAndUserId(documentId, target.getId()).isPresent()) {
            throw new BadRequestException("User is already a collaborator");
        }

        DocumentCollaborator collab = new DocumentCollaborator();
        collab.setDocument(doc);
        collab.setUser(target);
        collab.setPermission(request.getPermission() != null ? request.getPermission() : CollaboratorPermission.READ);
        collab.setAddedBy(actor);
        collaboratorRepository.save(collab);

        auditLogService.log(actor, "ADD_COLLABORATOR", "Document", doc.getId(),
                "Added " + target.getEmail() + " (" + collab.getPermission() + ") to " + doc.getTitle());
        return DocumentCollaboratorResponse.from(collab);
    }

    @Transactional
    public DocumentCollaboratorResponse updateCollaborator(Long documentId, UUID userId,
                                                            UpdateCollaboratorRequest request, User actor) {
        Document doc = findActiveDocument(documentId);
        accessService.requireEdit(actor, doc);

        DocumentCollaborator collab = collaboratorRepository.findByDocumentIdAndUserId(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaborator", "userId", userId));
        collab.setPermission(request.getPermission());
        collaboratorRepository.save(collab);

        auditLogService.log(actor, "UPDATE_COLLABORATOR", "Document", doc.getId(),
                "Set " + collab.getUser().getEmail() + " permission to " + request.getPermission());
        return DocumentCollaboratorResponse.from(collab);
    }

    @Transactional
    public void removeCollaborator(Long documentId, UUID userId, User actor) {
        Document doc = findActiveDocument(documentId);
        accessService.requireEdit(actor, doc);

        DocumentCollaborator collab = collaboratorRepository.findByDocumentIdAndUserId(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaborator", "userId", userId));
        collaboratorRepository.delete(collab);

        auditLogService.log(actor, "REMOVE_COLLABORATOR", "Document", doc.getId(),
                "Removed " + collab.getUser().getEmail() + " from " + doc.getTitle());
    }

    /**
     * Personal (no org) docs: PRIVATE or PUBLIC.
     * Org docs: ORG_INTERNAL or ORG_PUBLIC.
     */
    private void validateVisibility(Organization org, DocumentVisibility visibility) {
        if (org == null) {
            if (visibility != DocumentVisibility.PRIVATE && visibility != DocumentVisibility.PUBLIC) {
                throw new BadRequestException("Personal documents must be PRIVATE or PUBLIC");
            }
        } else {
            if (visibility != DocumentVisibility.ORG_INTERNAL && visibility != DocumentVisibility.ORG_PUBLIC) {
                throw new BadRequestException("Organization documents must be ORG_INTERNAL or ORG_PUBLIC");
            }
        }
    }

    private Document findActiveDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));
        if (document.isDeleted()) {
            throw new ResourceNotFoundException("Document", "id", documentId);
        }
        return document;
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
