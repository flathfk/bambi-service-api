package com.bambi.service.oauth.dto;

import jakarta.validation.constraints.NotNull;

public record OAuthAuthorizationDecisionRequest(@NotNull Boolean approved) {
}
