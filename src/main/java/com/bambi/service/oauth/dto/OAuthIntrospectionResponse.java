package com.bambi.service.oauth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OAuthIntrospectionResponse(
        boolean active,
        String sub,
        @JsonProperty("client_id") String clientId,
        String scope,
        Long exp,
        String aud
) {
    public static OAuthIntrospectionResponse inactive() {
        return new OAuthIntrospectionResponse(false, null, null, null, null, null);
    }
}
