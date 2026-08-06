package com.bambi.service.oauth;

import com.bambi.service.oauth.dto.OAuthIntrospectionRequest;
import com.bambi.service.oauth.dto.OAuthIntrospectionResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OAuthInternalController {

    private final OAuthService service;

    public OAuthInternalController(OAuthService service) {
        this.service = service;
    }

    @PostMapping("/internal/oauth/introspect")
    public OAuthIntrospectionResponse introspect(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody OAuthIntrospectionRequest request) {
        return service.introspect(authorization, request.token());
    }
}
