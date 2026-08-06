package com.bambi.service.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OAuthClientRegistrationRequest(
        @JsonProperty("client_name") String clientName,
        @JsonProperty("redirect_uris") List<String> redirectUris,
        @JsonProperty("token_endpoint_auth_method") String tokenEndpointAuthMethod,
        @JsonProperty("grant_types") List<String> grantTypes,
        @JsonProperty("response_types") List<String> responseTypes
) {
}
