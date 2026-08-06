package com.bambi.service.oauth.dto;

import java.time.OffsetDateTime;

public record OAuthAuthorizationRequestResponse(
        String requestId,
        String clientName,
        String clientOrigin,
        String scope,
        String resource,
        OffsetDateTime expiresAt
) {
}
