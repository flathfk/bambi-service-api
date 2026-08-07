package com.bambi.service.oauth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "oauth_tokens")
public class OAuthToken {

    @Id
    private UUID id;

    @Column(name = "client_id", nullable = false, length = 160)
    private String clientId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 500)
    private String scope;

    @Column(nullable = false, length = 1000)
    private String resource;

    @Column(name = "access_token_hash", nullable = false, length = 64)
    private String accessTokenHash;

    @Column(name = "refresh_token_hash", nullable = false, length = 64)
    private String refreshTokenHash;

    @Column(name = "access_expires_at", nullable = false)
    private OffsetDateTime accessExpiresAt;

    @Column(name = "refresh_expires_at", nullable = false)
    private OffsetDateTime refreshExpiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected OAuthToken() {
    }

    OAuthToken(String clientId, Long userId, String scope, String resource,
               String accessTokenHash, String refreshTokenHash,
               OffsetDateTime accessExpiresAt, OffsetDateTime refreshExpiresAt, OffsetDateTime now) {
        this.id = UUID.randomUUID();
        this.clientId = clientId;
        this.userId = userId;
        this.scope = scope;
        this.resource = resource;
        this.accessTokenHash = accessTokenHash;
        this.refreshTokenHash = refreshTokenHash;
        this.accessExpiresAt = accessExpiresAt;
        this.refreshExpiresAt = refreshExpiresAt;
        this.createdAt = now;
    }

    void rotate(String accessTokenHash, String refreshTokenHash,
                OffsetDateTime accessExpiresAt, OffsetDateTime refreshExpiresAt) {
        this.accessTokenHash = accessTokenHash;
        this.refreshTokenHash = refreshTokenHash;
        this.accessExpiresAt = accessExpiresAt;
        this.refreshExpiresAt = refreshExpiresAt;
    }

    void revoke(OffsetDateTime now) {
        if (revokedAt == null) revokedAt = now;
    }

    public UUID getId() { return id; }
    public String getClientId() { return clientId; }
    public Long getUserId() { return userId; }
    public String getScope() { return scope; }
    public String getResource() { return resource; }
    public OffsetDateTime getAccessExpiresAt() { return accessExpiresAt; }
    public OffsetDateTime getRefreshExpiresAt() { return refreshExpiresAt; }
    public OffsetDateTime getRevokedAt() { return revokedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
