package com.example.DocumentManagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "organization_members", uniqueConstraints = {
        @UniqueConstraint(name = "uk_org_member", columnNames = {"organization_id", "user_id"})
}, indexes = {
        @Index(name = "idx_org_member_user", columnList = "user_id"),
        @Index(name = "idx_org_member_org", columnList = "organization_id")
})
@Getter
@Setter
@NoArgsConstructor
public class OrganizationMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organization_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Organization organization;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "org_role", nullable = false, length = 16)
    private OrgRole orgRole = OrgRole.VIEWER;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }
}
