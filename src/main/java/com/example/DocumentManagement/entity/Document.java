package com.example.DocumentManagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_doc_owner", columnList = "created_by"),
        @Index(name = "idx_doc_org", columnList = "organization_id"),
        @Index(name = "idx_doc_visibility", columnList = "visibility"),
        @Index(name = "idx_doc_status", columnList = "status"),
        @Index(name = "idx_doc_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_doc_category", columnList = "category_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status = DocumentStatus.DRAFT;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organization_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DocumentVisibility visibility = DocumentVisibility.PRIVATE;

    @Column(name = "latest_object_key")
    private String latestObjectKey;

    @Column(name = "latest_version")
    private int latestVersion;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "document_tags", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "tag")
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<String> tags = new ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("versionNumber DESC")
    private List<DocumentVersion> versions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isPersonal() {
        return organization == null;
    }
}
