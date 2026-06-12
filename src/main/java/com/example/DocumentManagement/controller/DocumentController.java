package com.example.DocumentManagement.controller;

import com.example.DocumentManagement.dto.request.AddCollaboratorRequest;
import com.example.DocumentManagement.dto.request.DocumentRequest;
import com.example.DocumentManagement.dto.request.UpdateCollaboratorRequest;
import com.example.DocumentManagement.dto.request.WorkflowRequest;
import com.example.DocumentManagement.dto.response.*;
import com.example.DocumentManagement.entity.DocumentVisibility;
import com.example.DocumentManagement.entity.User;
import com.example.DocumentManagement.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Upload, list, version, and download documents (personal + organization)")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // Upload: orgId null → personal (PRIVATE); orgId set → org file (visibility required or defaulted)
    @Operation(summary = "Upload document with file")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponse>> createDocument(
            @AuthenticationPrincipal User user,
            @Parameter(description = "File to upload") @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam(value = "organizationId", required = false) UUID organizationId,
            @RequestParam(value = "visibility", required = false) DocumentVisibility visibility) {

        DocumentRequest request = new DocumentRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setCategoryId(categoryId);
        request.setTags(tags);
        request.setOrganizationId(organizationId);
        request.setVisibility(visibility);

        DocumentResponse response = documentService.createDocument(request, file, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Document created", response));
    }

    @Operation(summary = "List all documents visible to the current user (personal + accessible orgs)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> getDocuments(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Documents retrieved",
                documentService.getAccessibleDocuments(user, pageable)));
    }

    @Operation(summary = "List the current user's personal documents (PRIVATE + PUBLIC, own)")
    @GetMapping("/personal")
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> getPersonal(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Personal documents",
                documentService.getMyPersonalDocuments(user, pageable)));
    }

    @Operation(summary = "Discover PUBLIC personal files shared by other users")
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> getPublicPersonal(
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Public personal documents",
                documentService.getPublicPersonalDocuments(pageable)));
    }

    @Operation(summary = "List documents inside an organization (respects visibility + membership)")
    @GetMapping("/organization/{orgId}")
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> getByOrg(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Organization documents",
                documentService.getOrgDocuments(orgId, user, pageable)));
    }

    // Get document detail
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok("Document retrieved", documentService.getDocument(id, user)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentResponse>> updateDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok("Document updated",
                documentService.updateDocument(id, request, user)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        documentService.softDeleteDocument(id, user);
        return ResponseEntity.ok(ApiResponse.ok("Document moved to trash", null));
    }

    @Operation(summary = "Get a presigned MinIO download URL for the latest version")
    @GetMapping("/{id}/download")
    public ResponseEntity<ApiResponse<String>> downloadDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        String url = documentService.getDownloadUrl(id, user);
        return ResponseEntity.ok(ApiResponse.ok("Download URL", url));
    }

    // --- Versioning ---

    @Operation(summary = "Upload new version of document")
    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentVersionResponse>> uploadVersion(
            @PathVariable Long id,
            @AuthenticationPrincipal User user,
            @Parameter(description = "New version file") @RequestParam("file") MultipartFile file,
            @RequestParam("comment") String comment) {
        DocumentVersionResponse response = documentService.uploadNewVersion(id, file, comment, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("New version uploaded", response));
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<ApiResponse<List<DocumentVersionResponse>>> getVersionHistory(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok("Version history retrieved",
                documentService.getVersionHistory(id, user)));
    }

    @GetMapping("/{id}/versions/{versionNumber}/download")
    public ResponseEntity<ApiResponse<String>> downloadVersion(
            @PathVariable Long id,
            @PathVariable int versionNumber,
            @AuthenticationPrincipal User user) {
        String url = documentService.getVersionDownloadUrl(id, versionNumber, user);
        return ResponseEntity.ok(ApiResponse.ok("Download URL", url));
    }

    @PostMapping("/{id}/rollback")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentVersionResponse>> rollback(
            @PathVariable Long id,
            @RequestParam("targetVersion") int targetVersion,
            @RequestParam(value = "reason", required = false) String reason,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok("Rolled back successfully",
                documentService.rollback(id, targetVersion, reason, user)));
    }

    // --- Workflow ---

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<DocumentResponse>> submitForReview(
            @PathVariable Long id,
            @RequestBody(required = false) WorkflowRequest request,
            @AuthenticationPrincipal User user) {
        String comment = request != null ? request.getComment() : null;
        return ResponseEntity.ok(ApiResponse.ok("Document submitted for review",
                documentService.submitForReview(id, comment, user)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<DocumentResponse>> approve(
            @PathVariable Long id,
            @RequestBody(required = false) WorkflowRequest request,
            @AuthenticationPrincipal User user) {
        String comment = request != null ? request.getComment() : null;
        return ResponseEntity.ok(ApiResponse.ok("Document approved",
                documentService.approveDocument(id, comment, user)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<DocumentResponse>> reject(
            @PathVariable Long id,
            @RequestBody(required = false) WorkflowRequest request,
            @AuthenticationPrincipal User user) {
        String comment = request != null ? request.getComment() : null;
        return ResponseEntity.ok(ApiResponse.ok("Document rejected",
                documentService.rejectDocument(id, comment, user)));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<DocumentResponse>> archive(
            @PathVariable Long id,
            @RequestBody(required = false) WorkflowRequest request,
            @AuthenticationPrincipal User user) {
        String comment = request != null ? request.getComment() : null;
        return ResponseEntity.ok(ApiResponse.ok("Document archived",
                documentService.archiveDocument(id, comment, user)));
    }

    // --- Collaborators ---

    @Operation(summary = "List per-document collaborators (requires view permission)")
    @GetMapping("/{id}/collaborators")
    public ResponseEntity<ApiResponse<List<DocumentCollaboratorResponse>>> listCollaborators(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok("Collaborators",
                documentService.listCollaborators(id, user)));
    }

    @Operation(summary = "Add a collaborator by email with READ or WRITE permission (requires edit permission)")
    @PostMapping("/{id}/collaborators")
    public ResponseEntity<ApiResponse<DocumentCollaboratorResponse>> addCollaborator(
            @PathVariable Long id,
            @Valid @RequestBody AddCollaboratorRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Collaborator added",
                        documentService.addCollaborator(id, request, user)));
    }

    @Operation(summary = "Update a collaborator's permission")
    @PutMapping("/{id}/collaborators/{userId}")
    public ResponseEntity<ApiResponse<DocumentCollaboratorResponse>> updateCollaborator(
            @PathVariable Long id,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateCollaboratorRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok("Collaborator updated",
                documentService.updateCollaborator(id, userId, request, user)));
    }

    @Operation(summary = "Remove a collaborator")
    @DeleteMapping("/{id}/collaborators/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeCollaborator(
            @PathVariable Long id,
            @PathVariable UUID userId,
            @AuthenticationPrincipal User user) {
        documentService.removeCollaborator(id, userId, user);
        return ResponseEntity.ok(ApiResponse.ok("Collaborator removed", null));
    }

    @GetMapping("/{id}/workflow-history")
    public ResponseEntity<ApiResponse<List<WorkflowHistoryResponse>>> getWorkflowHistory(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok("Workflow history retrieved",
                documentService.getWorkflowHistory(id, user)));
    }
}
