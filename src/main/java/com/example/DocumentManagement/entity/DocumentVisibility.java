package com.example.DocumentManagement.entity;

public enum DocumentVisibility {
    /** Only the owner. Personal-scope only. */
    PRIVATE,
    /** Any authenticated user. Personal-scope only (share a personal file openly). */
    PUBLIC,
    /** Current members of the doc's org only. Org-scope only. */
    ORG_INTERNAL,
    /** Members + anyone if org itself is PUBLIC. Org-scope only. */
    ORG_PUBLIC
}
