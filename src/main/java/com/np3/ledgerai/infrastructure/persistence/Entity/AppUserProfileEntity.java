package com.np3.ledgerai.infrastructure.persistence.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Application-specific extension data for a keycloak-authenticated user.
 *
 * Keycloak is the source of truth for identity, this table deliberately holds none of that. Only relational business data that
 * would complicated to join if it lived in keycloak's admin API instead
 * */
@Entity
@Table(name = "app_user_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppUserProfileEntity {
    @Id
    @Column(name = "keycloak_subject_id", nullable = false, updatable = false)
    private String keycloakSubjectId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "company_id")
    private String companyId;

    @Column(name = "role_in_company")
    private String roleInCompany;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
