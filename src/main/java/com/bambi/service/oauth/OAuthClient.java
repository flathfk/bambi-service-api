package com.bambi.service.oauth;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "oauth_clients")
public class OAuthClient {

    @Id
    @Column(name = "client_id", length = 160)
    private String clientId;

    @Column(name = "client_name", nullable = false, length = 200)
    private String clientName;

    @Column(name = "token_endpoint_auth_method", nullable = false, length = 30)
    private String tokenEndpointAuthMethod;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "oauth_client_redirect_uris", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "redirect_uri", nullable = false, length = 1000)
    private Set<String> redirectUris = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected OAuthClient() {
    }

    OAuthClient(String clientId, String clientName, Set<String> redirectUris, OffsetDateTime createdAt) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.tokenEndpointAuthMethod = "none";
        this.redirectUris = new LinkedHashSet<>(redirectUris);
        this.createdAt = createdAt;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public Set<String> getRedirectUris() {
        return Set.copyOf(redirectUris);
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
