package com.example.DocumentManagement.controller;

import com.example.DocumentManagement.dto.request.AddMemberRequest;
import com.example.DocumentManagement.dto.request.CreateOrganizationRequest;
import com.example.DocumentManagement.dto.request.TransferOwnershipRequest;
import com.example.DocumentManagement.dto.request.UpdateMemberRoleRequest;
import com.example.DocumentManagement.dto.request.UpdateOrganizationRequest;
import com.example.DocumentManagement.dto.response.ApiResponse;
import com.example.DocumentManagement.dto.response.OrganizationMemberResponse;
import com.example.DocumentManagement.dto.response.OrganizationResponse;
import com.example.DocumentManagement.entity.User;
import com.example.DocumentManagement.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
@Tag(name = "Organizations", description = "Create and manage organizations, members and roles")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @Operation(summary = "Create organization (MANAGER or ADMIN). Creator becomes OWNER.")
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationResponse>> create(
            @Valid @RequestBody CreateOrganizationRequest request,
            @AuthenticationPrincipal User user) {
        OrganizationResponse res = organizationService.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Organization created", res));
    }

    @Operation(summary = "List organizations the current user is a member of")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<OrganizationResponse>>> listMine(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("My organizations", organizationService.listMine(user, pageable)));
    }

    @Operation(summary = "List all PUBLIC organizations (discoverable by anyone)")
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<Page<OrganizationResponse>>> listPublic(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Public organizations", organizationService.listPublic(pageable)));
    }

    @Operation(summary = "List ALL organizations (ADMIN only)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrganizationResponse>>> listAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("All organizations", organizationService.listAll(pageable)));
    }

    @Operation(summary = "Get organization detail (member, public, or ADMIN)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok("Organization", organizationService.get(id, user)));
    }

    @Operation(summary = "Update organization (org ADMIN/OWNER or system ADMIN)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrganizationRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok("Organization updated",
                organizationService.update(id, request, user)));
    }

    @Operation(summary = "Delete organization (org OWNER or system ADMIN)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id,
                                                     @AuthenticationPrincipal User user) {
        organizationService.delete(id, user);
        return ResponseEntity.ok(ApiResponse.ok("Organization deleted", null));
    }

    // --- Members ---

    @Operation(summary = "List members of an organization (member or ADMIN)")
    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<List<OrganizationMemberResponse>>> listMembers(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok("Members", organizationService.listMembers(id, user)));
    }

    @Operation(summary = "Add a member by email. Default role: VIEWER (org ADMIN/OWNER)")
    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<OrganizationMemberResponse>> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Member added", organizationService.addMember(id, request, user)));
    }

    @Operation(summary = "Change a member's role — VIEWER/EDITOR/ADMIN (org OWNER only)")
    @PutMapping("/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<OrganizationMemberResponse>> updateMemberRole(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok("Member role updated",
                organizationService.updateMemberRole(id, userId, request, user)));
    }

    @Operation(summary = "Transfer organization ownership to another member (current OWNER or system ADMIN)")
    @PostMapping("/{id}/transfer-ownership")
    public ResponseEntity<ApiResponse<OrganizationResponse>> transferOwnership(
            @PathVariable UUID id,
            @Valid @RequestBody TransferOwnershipRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok("Ownership transferred",
                organizationService.transferOwnership(id, request.getNewOwnerUserId(), user)));
    }

    @Operation(summary = "Remove a member from the organization (org ADMIN/OWNER)")
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @AuthenticationPrincipal User user) {
        organizationService.removeMember(id, userId, user);
        return ResponseEntity.ok(ApiResponse.ok("Member removed", null));
    }
}
