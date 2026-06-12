package com.example.DocumentManagement.service;

import com.example.DocumentManagement.dto.request.ChangePasswordRequest;
import com.example.DocumentManagement.dto.request.LoginRequest;
import com.example.DocumentManagement.dto.request.RegisterRequest;
import com.example.DocumentManagement.dto.request.UpdateUserRequest;
import com.example.DocumentManagement.dto.response.AuthResponse;
import com.example.DocumentManagement.dto.response.UserResponse;
import com.example.DocumentManagement.entity.Role;
import com.example.DocumentManagement.entity.User;
import com.example.DocumentManagement.exception.BadRequestException;
import com.example.DocumentManagement.exception.ResourceNotFoundException;
import com.example.DocumentManagement.repository.AuditLogRepository;
import com.example.DocumentManagement.repository.DocumentCollaboratorRepository;
import com.example.DocumentManagement.repository.DocumentRepository;
import com.example.DocumentManagement.repository.DocumentVersionRepository;
import com.example.DocumentManagement.repository.NotificationRepository;
import com.example.DocumentManagement.repository.OrganizationMemberRepository;
import com.example.DocumentManagement.repository.OrganizationRepository;
import com.example.DocumentManagement.repository.UserRepository;
import com.example.DocumentManagement.repository.WorkflowHistoryRepository;
import com.example.DocumentManagement.security.JwtTokenProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final AuditLogService auditLogService;
    private final AuditLogRepository auditLogRepository;
    private final NotificationRepository notificationRepository;
    private final WorkflowHistoryRepository workflowHistoryRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentCollaboratorRepository collaboratorRepository;
    private final OrganizationMemberRepository memberRepository;
    private final OrganizationRepository organizationRepository;
    private final DocumentRepository documentRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider, @Lazy AuthenticationManager authenticationManager,
                       AuditLogService auditLogService, AuditLogRepository auditLogRepository,
                       NotificationRepository notificationRepository,
                       WorkflowHistoryRepository workflowHistoryRepository,
                       DocumentVersionRepository documentVersionRepository,
                       DocumentCollaboratorRepository collaboratorRepository,
                       OrganizationMemberRepository memberRepository,
                       OrganizationRepository organizationRepository,
                       DocumentRepository documentRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.auditLogRepository = auditLogRepository;
        this.notificationRepository = notificationRepository;
        this.workflowHistoryRepository = workflowHistoryRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.memberRepository = memberRepository;
        this.organizationRepository = organizationRepository;
        this.documentRepository = documentRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setRole(Role.STAFF);
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user);

        auditLogService.log(user, "REGISTER", "User", null, "User registered");

        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = (User) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(user);

        auditLogService.log(user, "LOGIN", "User", null, "User logged in");

        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(), user.getRole().name());
    }

    public UserResponse createUser(com.example.DocumentManagement.dto.request.CreateUserRequest request, User admin) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());

        if (request.getRole() != null) {
            user.setRole(Role.valueOf(request.getRole().toUpperCase()));
        }

        userRepository.save(user);

        auditLogService.log(admin, "CREATE_USER", "User", null,
                "Created user: " + user.getEmail() + " with role: " + user.getRole());

        return UserResponse.from(user);
    }

    public UserResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return UserResponse.from(user);
    }

    public UserResponse updateProfile(UUID userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        userRepository.save(user);

        auditLogService.log(user, "UPDATE_PROFILE", "User", null, "Profile updated");

        return UserResponse.from(user);
    }

    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        auditLogService.log(user, "CHANGE_PASSWORD", "User", null, "Password changed");
    }

    public UserResponse updateRole(UUID userId, String role, User admin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Role newRole = Role.valueOf(role.toUpperCase());
        Role oldRole = user.getRole();
        user.setRole(newRole);
        userRepository.save(user);

        auditLogService.log(admin, "CHANGE_ROLE", "User", null,
                "Role changed from " + oldRole + " to " + newRole + " for " + user.getEmail());

        return UserResponse.from(user);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public void deleteUser(UUID userId, User admin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Block if user still owns an org — admin must transfer ownership first.
        if (organizationRepository.existsByOwnerId(userId)) {
            throw new BadRequestException(
                    "User owns one or more organizations. Transfer ownership before deleting.");
        }

        // Nullify historical references (keep records, remove user link).
        auditLogRepository.nullifyUser(userId);
        workflowHistoryRepository.nullifyPerformedBy(userId);
        documentVersionRepository.nullifyUploadedBy(userId);

        // Remove user's notifications, memberships, and collaborations.
        notificationRepository.deleteByUserId(userId);
        memberRepository.deleteByUserId(userId);
        collaboratorRepository.deleteByUserId(userId);

        // Soft-delete all documents owned by this user.
        documentRepository.softDeleteByOwnerId(userId);

        auditLogService.log(admin, "DELETE_USER", "User", null, "Deleted user: " + user.getEmail());
        userRepository.delete(user);
    }
}
