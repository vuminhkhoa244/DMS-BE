package com.example.DocumentManagement.entity;

public enum OrgRole {
    VIEWER,
    EDITOR,
    ADMIN,
    OWNER;

    public boolean canWrite() {
        return this == EDITOR || this == ADMIN || this == OWNER;
    }

    public boolean canManageMembers() {
        return this == ADMIN || this == OWNER;
    }

    public boolean isOwner() {
        return this == OWNER;
    }
}
