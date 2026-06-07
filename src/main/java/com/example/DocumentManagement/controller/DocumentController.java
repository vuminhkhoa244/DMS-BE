package com.example.DocumentManagement.controller;

import com.example.DocumentManagement.dto.request.DocumentRequest;
import com.example.DocumentManagement.dto.request.WorkflowRequest;
import com.example.DocumentManagement.dto.response.*;
import com.example.DocumentManagement.entity.DocumentStatus;
import com.example.DocumentManagement.entity.DocumentVersion;
import com.example.DocumentManagement.entity.User;
import com.example.DocumentManagement.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // US-05: Upload document
    @Operation(summary = "Upload document with file")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponse>> createDocument(
            @AuthenticationPrincipal User user,
            @Parameter(description = "File to upload (PDF, DOCX, XLSX, images, etc.)") @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "tags", required = false) List<String> tags) throws IOException {

        DocumentRequest request = new DocumentRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setCategoryId(categoryId);
        request.setTags(tags);

        DocumentResponse response = documentService.createDocument(request, file, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Document created", response));
    }

    // US-06: List documents (paginated, sorted by last modified)
    @GetMapping
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> getDocuments(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "status", required = false) DocumentStatus status,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<DocumentResponse> documents;
        if (tags != null && !tags.isEmpty()) {
            documents = documentService.searchByTags(tags, pageable);
        } else if (title != null || categoryId != null || status != null || from != null || to != null) {
            documents = documentService.search(title, categoryId, status, from, to, pageable);
        } else {
            documents = documentService.getDocuments(pageable);
        }
        return ResponseEntity.ok(ApiResponse.ok("Documents retrieved", documents));
    }

    // Get document detail
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocument(@PathVariable Long id) {
        DocumentResponse response = documentService.getDocument(id);
        return ResponseEntity.ok(ApiResponse.ok("Document retrieved", response));
    }

    // US-09: Update document metadata
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentResponse>> updateDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentRequest request,
            @AuthenticationPrincipal User user) {
        DocumentResponse response = documentService.updateDocument(id, request, user);
        return ResponseEntity.ok(ApiResponse.ok("Document updated", response));
    }

    // US-08: Soft delete document
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long id,
                                                             @AuthenticationPrincipal User user) {
        documentService.softDeleteDocument(id, user);
        return ResponseEntity.ok(ApiResponse.ok("Document moved to trash", null));
    }

    // US-07: Download document (latest version) - redirect to Cloudinary URL
    @GetMapping("/{id}/download")
    public ResponseEntity<Void> downloadDocument(@PathVariable Long id) {
        List<DocumentVersionResponse> versions = documentService.getVersionHistory(id);
        if (versions.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String fileUrl = versions.getFirst().getFileUrl();
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, fileUrl)
                .build();
    }

    // --- Versioning ---

    // US-10: Upload new version
    @Operation(summary = "Upload new version of document")
    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentVersionResponse>> uploadVersion(
            @PathVariable Long id,
            @AuthenticationPrincipal User user,
            @Parameter(description = "New version file") @RequestParam("file") MultipartFile file,
            @RequestParam("comment") String comment) throws IOException {
        DocumentVersionResponse response = documentService.uploadNewVersion(id, file, comment, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("New version uploaded", response));
    }

    // US-11: Version history
    @GetMapping("/{id}/versions")
    public ResponseEntity<ApiResponse<List<DocumentVersionResponse>>> getVersionHistory(@PathVariable Long id) {
        List<DocumentVersionResponse> versions = documentService.getVersionHistory(id);
        return ResponseEntity.ok(ApiResponse.ok("Version history retrieved", versions));
    }

    // US-12: Download specific version
    @GetMapping("/{id}/versions/{versionNumber}/download")
    public ResponseEntity<Void> downloadVersion(@PathVariable Long id,
                                                 @PathVariable int versionNumber) {
        DocumentVersion version = documentService.getVersion(id, versionNumber);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, version.getFileUrl())
                .build();
    }

    // US-13: Rollback version (Manager only)
    @PostMapping("/{id}/rollback")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentVersionResponse>> rollback(
            @PathVariable Long id,
            @RequestParam("targetVersion") int targetVersion,
            @RequestParam(value = "reason", required = false) String reason,
            @AuthenticationPrincipal User user) throws IOException {
        DocumentVersionResponse response = documentService.rollback(id, targetVersion, reason, user);
        return ResponseEntity.ok(ApiResponse.ok("Rolled back successfully", response));
    }

    // --- Workflow ---

    // US-18: Submit for review
    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<DocumentResponse>> submitForReview(
            @PathVariable Long id,
            @RequestBody(required = false) WorkflowRequest request,
            @AuthenticationPrincipal User user) {
        String comment = request != null ? request.getComment() : null;
        DocumentResponse response = documentService.submitForReview(id, comment, user);
        return ResponseEntity.ok(ApiResponse.ok("Document submitted for review", response));
    }

    // US-19: Approve document (Manager only)
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentResponse>> approve(
            @PathVariable Long id,
            @RequestBody(required = false) WorkflowRequest request,
            @AuthenticationPrincipal User user) {
        String comment = request != null ? request.getComment() : null;
        DocumentResponse response = documentService.approveDocument(id, comment, user);
        return ResponseEntity.ok(ApiResponse.ok("Document approved", response));
    }

    // US-19: Reject document (Manager only)
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentResponse>> reject(
            @PathVariable Long id,
            @RequestBody(required = false) WorkflowRequest request,
            @AuthenticationPrincipal User user) {
        String comment = request != null ? request.getComment() : null;
        DocumentResponse response = documentService.rejectDocument(id, comment, user);
        return ResponseEntity.ok(ApiResponse.ok("Document rejected", response));
    }

    // US-20: Archive document (Manager only)
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentResponse>> archive(
            @PathVariable Long id,
            @RequestBody(required = false) WorkflowRequest request,
            @AuthenticationPrincipal User user) {
        String comment = request != null ? request.getComment() : null;
        DocumentResponse response = documentService.archiveDocument(id, comment, user);
        return ResponseEntity.ok(ApiResponse.ok("Document archived", response));
    }

    // US-21: Workflow history
    @GetMapping("/{id}/workflow-history")
    public ResponseEntity<ApiResponse<List<WorkflowHistoryResponse>>> getWorkflowHistory(@PathVariable Long id) {
        List<WorkflowHistoryResponse> history = documentService.getWorkflowHistory(id);
        return ResponseEntity.ok(ApiResponse.ok("Workflow history retrieved", history));
    }
}
