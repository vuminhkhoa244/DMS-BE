package com.example.DocumentManagement.service;

import com.example.DocumentManagement.dto.request.AddMemberRequest;
import com.example.DocumentManagement.dto.request.CreateOrganizationRequest;
import com.example.DocumentManagement.dto.request.UpdateMemberRoleRequest;
import com.example.DocumentManagement.dto.request.UpdateOrganizationRequest;
import com.example.DocumentManagement.dto.response.OrganizationMemberResponse;
import com.example.DocumentManagement.dto.response.OrganizationResponse;
import com.example.DocumentManagement.entity.*;
import com.example.DocumentManagement.exception.BadRequestException;
import com.example.DocumentManagement.exception.ResourceNotFoundException;
import com.example.DocumentManagement.repository.DocumentRepository;
import com.example.DocumentManagement.repository.OrganizationMemberRepository;
import com.example.DocumentManagement.repository.OrganizationRepository;
import com.example.DocumentManagement.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrganizationService {

    private final OrganizationRepository orgRepository;
    private final OrganizationMemberRepository memberRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public OrganizationService(OrganizationRepository orgRepository,
                               OrganizationMemberRepository memberRepository,
                               DocumentRepository documentRepository,
                               UserRepository userRepository,
                               AuditLogService auditLogService) {
        this.orgRepository = orgRepository;
        this.memberRepository = memberRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public OrganizationResponse create(CreateOrganizationRequest request, User creator) {
        if (creator.getRole() != Role.MANAGER && creator.getRole() != Role.ADMIN) {
            throw new BadRequestException("Only managers or admins can create organizations");
        }
        if (orgRepository.existsBySlug(request.getSlug())) {
            throw new BadRequestException("Slug already in use");
        }

        Organization org = new Organization();
        org.setName(request.getName());
        org.setSlug(request.getSlug());
        org.setDescription(request.getDescription());
        org.setVisibility(request.getVisibility() != null ? request.getVisibility() : OrgVisibility.PRIVATE);
        org.setOwner(creator);
        orgRepository.save(org);

        OrganizationMember ownerMember = new OrganizationMember();
        ownerMember.setOrganization(org);
        ownerMember.setUser(creator);
        ownerMember.setOrgRole(OrgRole.OWNER);
        memberRepository.save(ownerMember);

        auditLogService.log(creator, "CREATE_ORG", "Organization", null,
                "Created organization: " + org.getName() + " (" + org.getSlug() + ")");

        return OrganizationResponse.from(org);
    }

    @Transactional
    public OrganizationResponse update(UUID orgId, UpdateOrganizationRequest request, User user) {
        Organization org = getOrgOrThrow(orgId);
        requireOrgAdmin(org, user);

        if (request.getName() != null) org.setName(request.getName());
        if (request.getDescription() != null) org.setDescription(request.getDescription());
        if (request.getVisibility() != null) org.setVisibility(request.getVisibility());
        orgRepository.save(org);

        auditLogService.log(user, "UPDATE_ORG", "Organization", null,
                "Updated organization: " + org.getName());
        return OrganizationResponse.from(org);
    }

    @Transactional
    public void delete(UUID orgId, User user) {
        Organization org = getOrgOrThrow(orgId);
        boolean isSystemAdmin = user.getRole() == Role.ADMIN;
        boolean isOrgOwner = org.getOwner().getId().equals(user.getId());
        if (!isSystemAdmin && !isOrgOwner) {
            throw new BadRequestException("Only org owner or system admin can delete organization");
        }
        // Detach documents from org before deletion (docs become personal files of their owners)
        documentRepository.detachFromOrganization(orgId);
        // Remove all members
        memberRepository.deleteByOrganizationId(orgId);
        orgRepository.delete(org);
        auditLogService.log(user, "DELETE_ORG", "Organization", null,
                "Deleted organization: " + org.getName());
    }

    public OrganizationResponse get(UUID orgId, User user) {
        Organization org = getOrgOrThrow(orgId);
        boolean isMember = memberRepository.existsByOrganizationIdAndUserId(orgId, user.getId());
        boolean isSystemAdmin = user.getRole() == Role.ADMIN;
        if (!isMember && !isSystemAdmin && org.getVisibility() != OrgVisibility.PUBLIC) {
            throw new BadRequestException("You don't have access to this organization");
        }
        return OrganizationResponse.from(org);
    }

    public Page<OrganizationResponse> listMine(User user, Pageable pageable) {
        return orgRepository.findAllByMemberUserId(user.getId(), pageable)
                .map(OrganizationResponse::from);
    }

    public Page<OrganizationResponse> listPublic(Pageable pageable) {
        return orgRepository.findByVisibility(OrgVisibility.PUBLIC, pageable)
                .map(OrganizationResponse::from);
    }

    public Page<OrganizationResponse> listAll(Pageable pageable) {
        return orgRepository.findAll(pageable).map(OrganizationResponse::from);
    }

    // --- Members ---

    @Transactional
    public OrganizationMemberResponse addMember(UUID orgId, AddMemberRequest request, User actor) {
        Organization org = getOrgOrThrow(orgId);
        requireOrgAdmin(org, actor);

        User target = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        if (memberRepository.existsByOrganizationIdAndUserId(orgId, target.getId())) {
            throw new BadRequestException("User is already a member");
        }

        OrgRole role = request.getOrgRole() != null ? request.getOrgRole() : OrgRole.VIEWER;
        if (role == OrgRole.OWNER) {
            throw new BadRequestException("Cannot assign OWNER role via add member; use transfer ownership");
        }

        OrganizationMember member = new OrganizationMember();
        member.setOrganization(org);
        member.setUser(target);
        member.setOrgRole(role);
        memberRepository.save(member);

        auditLogService.log(actor, "ADD_ORG_MEMBER", "Organization", null,
                "Added " + target.getEmail() + " as " + role + " to " + org.getName());
        return OrganizationMemberResponse.from(member);
    }

    @Transactional
    public OrganizationMemberResponse updateMemberRole(UUID orgId, UUID userId,
                                                       UpdateMemberRoleRequest request, User actor) {
        Organization org = getOrgOrThrow(orgId);
        requireOrgOwner(org, actor);

        OrganizationMember member = memberRepository.findByOrganizationIdAndUserId(orgId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", "userId", userId));

        if (member.getOrgRole() == OrgRole.OWNER) {
            throw new BadRequestException("Cannot change OWNER role");
        }
        if (request.getOrgRole() == OrgRole.OWNER) {
            throw new BadRequestException("Use transfer-ownership to change owner");
        }

        member.setOrgRole(request.getOrgRole());
        memberRepository.save(member);

        auditLogService.log(actor, "UPDATE_ORG_MEMBER", "Organization", null,
                "Set role of " + member.getUser().getEmail() + " to " + request.getOrgRole());
        return OrganizationMemberResponse.from(member);
    }

    @Transactional
    public OrganizationResponse transferOwnership(UUID orgId, UUID newOwnerUserId, User actor) {
        Organization org = getOrgOrThrow(orgId);
        boolean isSystemAdmin = actor.getRole() == Role.ADMIN;
        boolean isCurrentOwner = org.getOwner().getId().equals(actor.getId());
        if (!isSystemAdmin && !isCurrentOwner) {
            throw new BadRequestException("Only the current owner or a system admin can transfer ownership");
        }

        if (newOwnerUserId.equals(org.getOwner().getId())) {
            throw new BadRequestException("User is already the owner");
        }

        OrganizationMember newOwnerMember = memberRepository
                .findByOrganizationIdAndUserId(orgId, newOwnerUserId)
                .orElseThrow(() -> new BadRequestException("Target user is not a member of this organization"));

        OrganizationMember oldOwnerMember = memberRepository
                .findByOrganizationIdAndUserId(orgId, org.getOwner().getId())
                .orElseThrow(() -> new BadRequestException("Current owner membership not found"));

        // Swap roles atomically
        oldOwnerMember.setOrgRole(OrgRole.ADMIN);
        newOwnerMember.setOrgRole(OrgRole.OWNER);
        memberRepository.save(oldOwnerMember);
        memberRepository.save(newOwnerMember);

        org.setOwner(newOwnerMember.getUser());
        orgRepository.save(org);

        auditLogService.log(actor, "TRANSFER_ORG_OWNERSHIP", "Organization", null,
                "Transferred " + org.getName() + " ownership to " + newOwnerMember.getUser().getEmail());
        return OrganizationResponse.from(org);
    }

    @Transactional
    public void removeMember(UUID orgId, UUID userId, User actor) {
        Organization org = getOrgOrThrow(orgId);
        requireOrgAdmin(org, actor);

        OrganizationMember member = memberRepository.findByOrganizationIdAndUserId(orgId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", "userId", userId));
        if (member.getOrgRole() == OrgRole.OWNER) {
            throw new BadRequestException("Cannot remove OWNER");
        }

        memberRepository.delete(member);
        auditLogService.log(actor, "REMOVE_ORG_MEMBER", "Organization", null,
                "Removed " + member.getUser().getEmail() + " from " + org.getName());
    }

    public List<OrganizationMemberResponse> listMembers(UUID orgId, User user) {
        Organization org = getOrgOrThrow(orgId);
        boolean isMember = memberRepository.existsByOrganizationIdAndUserId(orgId, user.getId());
        boolean isSystemAdmin = user.getRole() == Role.ADMIN;
        if (!isMember && !isSystemAdmin) {
            throw new BadRequestException("You don't have access to this organization");
        }
        return memberRepository.findByOrganizationId(orgId).stream()
                .map(OrganizationMemberResponse::from)
                .toList();
    }

    // --- Helpers ---

    private Organization getOrgOrThrow(UUID orgId) {
        return orgRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));
    }

    private void requireOrgAdmin(Organization org, User user) {
        if (user.getRole() == Role.ADMIN) return;
        OrganizationMember m = memberRepository.findByOrganizationIdAndUserId(org.getId(), user.getId())
                .orElseThrow(() -> new BadRequestException("Not a member of this organization"));
        if (!m.getOrgRole().canManageMembers()) {
            throw new BadRequestException("Requires ADMIN role in this organization");
        }
    }

    private void requireOrgOwner(Organization org, User user) {
        if (user.getRole() == Role.ADMIN) return;
        if (!org.getOwner().getId().equals(user.getId())) {
            throw new BadRequestException("Only org owner can perform this action");
        }
    }
}
