package com.example.DocumentManagement.service;

import com.example.DocumentManagement.entity.*;
import com.example.DocumentManagement.exception.BadRequestException;
import com.example.DocumentManagement.entity.DocumentStatus;
import com.example.DocumentManagement.repository.DocumentCollaboratorRepository;
import com.example.DocumentManagement.repository.OrganizationMemberRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Centralized access control for documents and organizations.
 *
 *  Document visibility rules:
 *   - PRIVATE       : only the owner
 *   - ORG_INTERNAL  : any member of the document's org
 *   - ORG_PUBLIC    : any member; plus any authenticated user if the org itself is PUBLIC
 *
 *  Write/delete rules within an org:
 *   - VIEWER : no write
 *   - EDITOR : can edit/delete OWN documents
 *   - ADMIN  : can edit/delete ANY document in the org
 *   - OWNER  : full control
 *
 *  System ADMIN can do anything.
 */
@Service
public class DocumentAccessService {

    private final OrganizationMemberRepository memberRepository;
    private final DocumentCollaboratorRepository collaboratorRepository;

    public DocumentAccessService(OrganizationMemberRepository memberRepository,
                                 DocumentCollaboratorRepository collaboratorRepository) {
        this.memberRepository = memberRepository;
        this.collaboratorRepository = collaboratorRepository;
    }

    public boolean canView(User user, Document doc) {
        if (user == null || doc == null || doc.isDeleted()) return false;
        if (user.getRole() == Role.ADMIN) return true;

        boolean isOwner = doc.getCreatedBy().getId().equals(user.getId());
        boolean isPublished = doc.getStatus() == DocumentStatus.APPROVED
                           || doc.getStatus() == DocumentStatus.ARCHIVED;

        // Collaborator always has view access regardless of status.
        if (collaboratorRepository.findByDocumentIdAndUserId(doc.getId(), user.getId()).isPresent()) {
            return true;
        }

        // Personal file (no org)
        if (doc.getOrganization() == null) {
            if (isOwner) return true;
            // Non-owners only see personal PUBLIC docs that are APPROVED.
            return isPublished && doc.getVisibility() == DocumentVisibility.PUBLIC;
        }

        // Org file
        Organization org = doc.getOrganization();
        Optional<OrganizationMember> membershipOpt =
                memberRepository.findByOrganizationIdAndUserId(org.getId(), user.getId());

        // Doc owner who is still a member always sees their own doc.
        if (isOwner && membershipOpt.isPresent()) return true;

        // Org ADMIN/OWNER can see any status (for review purposes).
        if (membershipOpt.isPresent() && membershipOpt.get().getOrgRole().canManageMembers()) return true;

        // Everyone else requires APPROVED or ARCHIVED.
        if (!isPublished) return false;

        switch (doc.getVisibility()) {
            case PRIVATE:
                return false;
            case ORG_INTERNAL:
                return membershipOpt.isPresent();
            case ORG_PUBLIC:
                if (membershipOpt.isPresent()) return true;
                return org.getVisibility() == OrgVisibility.PUBLIC;
            default:
                return false;
        }
    }

    public boolean canEdit(User user, Document doc) {
        if (user == null || doc == null || doc.isDeleted()) return false;
        if (user.getRole() == Role.ADMIN) return true;

        // ARCHIVED documents are read-only for everyone.
        if (doc.getStatus() == DocumentStatus.ARCHIVED) return false;

        boolean isOwner = doc.getCreatedBy().getId().equals(user.getId());

        // Per-document collaborator with WRITE permission.
        var collaborator = collaboratorRepository.findByDocumentIdAndUserId(doc.getId(), user.getId());
        if (collaborator.isPresent() && collaborator.get().getPermission() == CollaboratorPermission.WRITE) {
            return true;
        }

        // Personal file: owner only (collaborators handled above)
        if (doc.getOrganization() == null) return isOwner;

        // Org file: must currently be a member; ex-members lose write rights.
        OrganizationMember m = memberRepository
                .findByOrganizationIdAndUserId(doc.getOrganization().getId(), user.getId())
                .orElse(null);
        if (m == null) return false;

        // EDITOR can modify their own; ADMIN/OWNER can modify anything.
        if (isOwner && m.getOrgRole().canWrite()) return true;
        return m.getOrgRole() == OrgRole.ADMIN || m.getOrgRole() == OrgRole.OWNER;
    }

    public boolean canDelete(User user, Document doc) {
        // Same rules as edit for now.
        return canEdit(user, doc);
    }

    /**
     * Check if the user can upload a new document into the given org (or personal space if orgId null).
     */
    public void requireCanUpload(User user, UUID orgId) {
        if (orgId == null) return; // personal space, always allowed
        if (user.getRole() == Role.ADMIN) return;

        OrganizationMember m = memberRepository.findByOrganizationIdAndUserId(orgId, user.getId())
                .orElseThrow(() -> new BadRequestException("You are not a member of this organization"));
        if (!m.getOrgRole().canWrite()) {
            throw new BadRequestException("Your role in this organization does not allow uploads");
        }
    }

    public void requireView(User user, Document doc) {
        if (!canView(user, doc)) {
            throw new BadRequestException("You do not have permission to view this document");
        }
    }

    public void requireEdit(User user, Document doc) {
        if (!canEdit(user, doc)) {
            throw new BadRequestException("You do not have permission to modify this document");
        }
    }

    public void requireDelete(User user, Document doc) {
        if (!canDelete(user, doc)) {
            throw new BadRequestException("You do not have permission to delete this document");
        }
    }
}
