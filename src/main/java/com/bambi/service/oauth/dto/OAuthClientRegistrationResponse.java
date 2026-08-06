package com.bambi.service.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OAuthClientRegistrationResponse(
        @JsonProperty("client_id") String clientId,
        @JsonProperty("client_name") String clientName,
        @JsonProperty("redirect_uris") List<String> redirectUris,
        @JsonProperty("token_endpoint_auth_method") String tokenEndpointAuthMethod,
        @JsonProperty("grant_types") List<String> grantTypes,
        @JsonProperty("response_types") List<String> responseTypes,
        @JsonProperty("client_id_issued_at") long clientIdIssuedAt
) {
}
