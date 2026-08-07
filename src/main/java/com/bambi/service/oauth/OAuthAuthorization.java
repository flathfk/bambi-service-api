package com.bambi.service.oauth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "oauth_authorizations")
public class OAuthAuthorization {

    @Id
    @Column(length = 80)
    private String id;

    @Column(name = "client_id", nullable = false, length = 160)
    private String clientId;

    @Column(name = "redirect_uri", nullable = false, length = 1000)
    private String redirectUri;

    @Column(length = 1000)
    private String state;

    @Column(nullable = false, length = 500)
    private String scope;

    @Column(nullable = false, length = 1000)
    private String resource;

    @Column(name = "code_challenge", nullable = false, length = 160)
    private String codeChallenge;

    @Column(name = "code_challenge_method", nullable = false, length = 10)
    private String codeChallengeMethod;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "authorization_code_hash", length = 64)
    private String authorizationCodeHash;

    @Column(name = "code_expires_at")
    private OffsetDateTime codeExpiresAt;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected OAuthAuthorization() {
    }

    OAuthAuthorization(String id, String clientId, String redirectUri, String state, String scope,
                       String resource, String codeChallenge, OffsetDateTime now, OffsetDateTime expiresAt) {
        this.id = id;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.state = state;
        this.scope = scope;
        this.resource = resource;
        this.codeChallenge = codeChallenge;
        this.codeChallengeMethod = "S256";
        this.status = "PENDING";
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    void approve(Long userId, String codeHash, OffsetDateTime codeExpiresAt) {
        this.userId = userId;
        this.authorizationCodeHash = codeHash;
        this.codeExpiresAt = codeExpiresAt;
        this.status = "APPROVED";
    }

    void deny() {
        this.status = "DENIED";
    }

    void consume(OffsetDateTime consumedAt) {
        this.consumedAt = consumedAt;
        this.status = "CONSUMED";
    }

    public String getId() { return id; }
    public String getClientId() { return clientId; }
    public String getRedirectUri() { return redirectUri; }
    public String getState() { return state; }
    public String getScope() { return scope; }
    public String getResource() { return resource; }
    public String getCodeChallenge() { return codeChallenge; }
    public Long getUserId() { return userId; }
    public String getStatus() { return status; }
    public OffsetDateTime getCodeExpiresAt() { return codeExpiresAt; }
    public OffsetDateTime getConsumedAt() { return consumedAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
}
