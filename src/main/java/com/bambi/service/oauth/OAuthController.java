package com.bambi.service.oauth;

import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.common.response.ApiResponse;
import com.bambi.service.oauth.dto.OAuthAuthorizationDecisionRequest;
import com.bambi.service.oauth.dto.OAuthAuthorizationDecisionResponse;
import com.bambi.service.oauth.dto.OAuthAuthorizationRequestResponse;
import com.bambi.service.oauth.dto.OAuthClientRegistrationRequest;
import com.bambi.service.oauth.dto.OAuthClientRegistrationResponse;
import com.bambi.service.oauth.dto.OAuthTokenResponse;
import com.bambi.service.oauth.dto.OAuthConnectionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;
import java.util.List;
import java.util.UUID;

@RestController
public class OAuthController {

    private final OAuthService service;

    public OAuthController(OAuthService service) {
        this.service = service;
    }

    @GetMapping("/.well-known/oauth-authorization-server")
    public Map<String, Object> metadata() {
        return service.authorizationServerMetadata();
    }

    @PostMapping("/api/oauth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public OAuthClientRegistrationResponse register(@RequestBody OAuthClientRegistrationRequest request) {
        return service.register(request);
    }

    @GetMapping("/api/oauth/authorize")
    public ResponseEntity<Void> authorize(
            @RequestParam(name = "response_type") String responseType,
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "redirect_uri") String redirectUri,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String state,
            @RequestParam(name = "code_challenge") String codeChallenge,
            @RequestParam(name = "code_challenge_method") String codeChallengeMethod,
            @RequestParam String resource) {
        try {
            String location = service.beginAuthorization(responseType, clientId, redirectUri, scope, state,
                    codeChallenge, codeChallengeMethod, resource);
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(location)).build();
        } catch (OAuthRedirectException e) {
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(e.getRedirectUrl())).build();
        }
    }

    @GetMapping("/api/oauth/authorization-requests/{requestId}")
    public ApiResponse<OAuthAuthorizationRequestResponse> authorizationRequest(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String requestId) {
        return ApiResponse.ok(service.getAuthorizationRequest(principal.id(), requestId));
    }

    @PostMapping("/api/oauth/authorization-requests/{requestId}/decision")
    public ApiResponse<OAuthAuthorizationDecisionResponse> decide(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String requestId,
            @Valid @RequestBody OAuthAuthorizationDecisionRequest request) {
        return ApiResponse.ok(service.decide(principal.id(), requestId, request.approved()));
    }

    @GetMapping("/api/oauth/connections")
    public ApiResponse<List<OAuthConnectionResponse>> connections(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(service.listConnections(principal.id()));
    }

    @DeleteMapping("/api/oauth/connections/{connectionId}")
    public ApiResponse<OAuthConnectionResponse> revokeConnection(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID connectionId) {
        return ApiResponse.ok(service.revokeConnection(principal.id(), connectionId));
    }

    @PostMapping(value = "/api/oauth/token", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<OAuthTokenResponse> token(@RequestParam MultiValueMap<String, String> form) {
        String grantType = form.getFirst("grant_type");
        OAuthTokenResponse token;
        if ("authorization_code".equals(grantType)) {
            token = service.exchangeAuthorizationCode(
                    form.getFirst("code"), form.getFirst("client_id"), form.getFirst("redirect_uri"),
                    form.getFirst("code_verifier"), form.getFirst("resource"));
        } else if ("refresh_token".equals(grantType)) {
            token = service.refresh(form.getFirst("refresh_token"), form.getFirst("client_id"),
                    form.getFirst("resource"));
        } else {
            throw new OAuthProtocolException("unsupported_grant_type", "지원하지 않는 grant_type입니다.");
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(token);
    }

    @PostMapping(value = "/api/oauth/revoke", consumes = "application/x-www-form-urlencoded")
    @ResponseStatus(HttpStatus.OK)
    public void revoke(@RequestParam MultiValueMap<String, String> form) {
        service.revoke(form.getFirst("token"), form.getFirst("client_id"));
    }
}
