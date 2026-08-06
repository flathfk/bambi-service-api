package com.bambi.service.oauth.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OAuthConnectionResponse(
        UUID id,
        String clientName,
        String scope,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt,
        OffsetDateTime revokedAt
) {
}
