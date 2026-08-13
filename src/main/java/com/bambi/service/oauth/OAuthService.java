package com.bambi.service.oauth;

import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.oauth.dto.OAuthAuthorizationDecisionResponse;
import com.bambi.service.oauth.dto.OAuthAuthorizationRequestResponse;
import com.bambi.service.oauth.dto.OAuthClientRegistrationRequest;
import com.bambi.service.oauth.dto.OAuthClientRegistrationResponse;
import com.bambi.service.oauth.dto.OAuthIntrospectionResponse;
import com.bambi.service.oauth.dto.OAuthTokenResponse;
import com.bambi.service.oauth.dto.OAuthConnectionResponse;
import com.bambi.service.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class OAuthService {

    private static final String READ_SCOPE = "wiki:read";
    private static final String WRITE_SCOPE = "wiki:write";
    private static final Set<String> SUPPORTED_SCOPES = Set.of(READ_SCOPE, WRITE_SCOPE);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OAuthClientRepository clientRepository;
    private final OAuthAuthorizationRepository authorizationRepository;
    private final OAuthTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final String issuerUrl;
    private final String authorizationPageUrl;
    private final String resourceUrl;
    private final String internalToken;
    private final long authorizationRequestMinutes;
    private final long authorizationCodeMinutes;
    private final long accessTokenMinutes;
    private final long refreshTokenDays;

    public OAuthService(
            OAuthClientRepository clientRepository,
            OAuthAuthorizationRepository authorizationRepository,
            OAuthTokenRepository tokenRepository,
            UserRepository userRepository,
            @Value("${app.oauth.issuer-url}") String issuerUrl,
            @Value("${app.oauth.authorization-page-url}") String authorizationPageUrl,
            @Value("${app.oauth.resource-url}") String resourceUrl,
            @Value("${app.agent.internal-token:}") String internalToken,
            @Value("${app.oauth.authorization-request-minutes:10}") long authorizationRequestMinutes,
            @Value("${app.oauth.authorization-code-minutes:5}") long authorizationCodeMinutes,
            @Value("${app.oauth.access-token-minutes:60}") long accessTokenMinutes,
            @Value("${app.oauth.refresh-token-days:30}") long refreshTokenDays) {
        this.clientRepository = clientRepository;
        this.authorizationRepository = authorizationRepository;
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        // MCP SDK의 AnyHttpUrl은 origin-only URL을 trailing slash 형태로 정규화한다.
        // Protected Resource Metadata와 Authorization Server Metadata의 issuer를 정확히 맞춘다.
        this.issuerUrl = stripTrailingSlash(issuerUrl) + "/";
        this.authorizationPageUrl = stripTrailingSlash(authorizationPageUrl);
        this.resourceUrl = resourceUrl;
        this.internalToken = internalToken;
        this.authorizationRequestMinutes = authorizationRequestMinutes;
        this.authorizationCodeMinutes = authorizationCodeMinutes;
        this.accessTokenMinutes = accessTokenMinutes;
        this.refreshTokenDays = refreshTokenDays;
    }

    public Map<String, Object> authorizationServerMetadata() {
        String endpointBase = stripTrailingSlash(issuerUrl);
        return Map.of(
                "issuer", issuerUrl,
                "authorization_endpoint", endpointBase + "/api/oauth/authorize",
                "token_endpoint", endpointBase + "/api/oauth/token",
                "registration_endpoint", endpointBase + "/api/oauth/register",
                "revocation_endpoint", endpointBase + "/api/oauth/revoke",
                "response_types_supported", List.of("code"),
                "grant_types_supported", List.of("authorization_code", "refresh_token"),
                "token_endpoint_auth_methods_supported", List.of("none"),
                "code_challenge_methods_supported", List.of("S256"),
                "scopes_supported", List.of(READ_SCOPE, WRITE_SCOPE)
        );
    }

    @Transactional
    public OAuthClientRegistrationResponse register(OAuthClientRegistrationRequest request) {
        validateRegistration(request);
        OffsetDateTime now = now();
        Set<String> redirectUris = new LinkedHashSet<>(request.redirectUris());
        OAuthClient client = new OAuthClient(
                "bmb_client_" + randomValue(24), request.clientName().trim(), redirectUris, now);
        clientRepository.save(client);
        return new OAuthClientRegistrationResponse(
                client.getClientId(), client.getClientName(), List.copyOf(redirectUris), "none",
                List.of("authorization_code", "refresh_token"), List.of("code"),
                now.toEpochSecond());
    }

    @Transactional
    public String beginAuthorization(String responseType, String clientId, String redirectUri,
                                     String scope, String state, String codeChallenge,
                                     String codeChallengeMethod, String resource) {
        if (!"code".equals(responseType)) {
            throw protocol("unsupported_response_type", "response_type은 code만 지원합니다.");
        }
        if (clientId == null || clientId.isBlank() || clientId.length() > 160
                || redirectUri == null || redirectUri.isBlank() || redirectUri.length() > 1000) {
            throw protocol("invalid_request", "client_id와 redirect_uri가 필요합니다.");
        }
        OAuthClient client = clientRepository.findById(clientId)
                .orElseThrow(() -> protocol("invalid_request", "등록되지 않은 client_id입니다."));
        if (!client.getRedirectUris().contains(redirectUri)) {
            throw protocol("invalid_request", "redirect_uri가 등록값과 일치하지 않습니다.");
        }
        if (state != null && state.length() > 1000) {
            throw redirectError(redirectUri, null, "invalid_request", "state가 너무 깁니다.");
        }
        String normalizedScope = normalizeScope(scope);
        Set<String> requestedScopes = Set.of(normalizedScope.split(" "));
        if (!SUPPORTED_SCOPES.containsAll(requestedScopes)
                || requestedScopes.contains(WRITE_SCOPE) && !requestedScopes.contains(READ_SCOPE)) {
            throw redirectError(
                    redirectUri,
                    state,
                    "invalid_scope",
                    "wiki:write는 wiki:read와 함께 요청해야 합니다."
            );
        }
        if (!"S256".equals(codeChallengeMethod) || !isPkceValue(codeChallenge)) {
            throw redirectError(redirectUri, state, "invalid_request", "PKCE S256 code_challenge가 필요합니다.");
        }
        if (!resourceUrl.equals(resource)) {
            throw redirectError(redirectUri, state, "invalid_target", "resource가 MCP URL과 일치하지 않습니다.");
        }

        OffsetDateTime now = now();
        OAuthAuthorization authorization = new OAuthAuthorization(
                "bmb_auth_" + randomValue(24), clientId, redirectUri, state, normalizedScope, resource,
                codeChallenge, now, now.plusMinutes(authorizationRequestMinutes));
        authorizationRepository.save(authorization);
        return UriComponentsBuilder.fromUriString(authorizationPageUrl)
                .queryParam("request_id", authorization.getId())
                .build().encode().toUriString();
    }

    @Transactional(readOnly = true)
    public OAuthAuthorizationRequestResponse getAuthorizationRequest(Long userId, String requestId) {
        requireActiveUser(userId);
        OAuthAuthorization authorization = authorizationRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "OAuth 승인 요청을 찾을 수 없습니다."));
        if (!"PENDING".equals(authorization.getStatus()) || authorization.getExpiresAt().isBefore(now())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "OAuth 승인 요청이 만료되었거나 이미 처리되었습니다.");
        }
        OAuthClient client = clientRepository.findById(authorization.getClientId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        return new OAuthAuthorizationRequestResponse(
                authorization.getId(), client.getClientName(), clientOrigin(authorization.getRedirectUri()),
                authorization.getScope(),
                authorization.getResource(), authorization.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public List<OAuthConnectionResponse> listConnections(Long userId) {
        requireActiveUser(userId);
        OffsetDateTime now = now();
        return tokenRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(token -> toConnection(token, now))
                .toList();
    }

    @Transactional
    public OAuthConnectionResponse revokeConnection(Long userId, UUID connectionId) {
        requireActiveUser(userId);
        OAuthToken token = tokenRepository.findByIdAndUserId(connectionId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "OAuth 연결을 찾을 수 없습니다."));
        token.revoke(now());
        return toConnection(token, now());
    }

    @Transactional
    public OAuthAuthorizationDecisionResponse decide(Long userId, String requestId, boolean approved) {
        requireActiveUser(userId);
        OAuthAuthorization authorization = authorizationRepository.findLockedById(requestId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "OAuth 승인 요청을 찾을 수 없습니다."));
        OffsetDateTime now = now();
        if (!"PENDING".equals(authorization.getStatus()) || authorization.getExpiresAt().isBefore(now)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "OAuth 승인 요청이 만료되었거나 이미 처리되었습니다.");
        }

        if (!approved) {
            authorization.deny();
            return new OAuthAuthorizationDecisionResponse(callback(
                    authorization.getRedirectUri(), authorization.getState(),
                    "error", "access_denied", "error_description", "사용자가 연결을 거부했습니다."));
        }

        String code = "bmb_code_" + randomValue(32);
        authorization.approve(userId, hash(code), now.plusMinutes(authorizationCodeMinutes));
        return new OAuthAuthorizationDecisionResponse(callback(
                authorization.getRedirectUri(), authorization.getState(), "code", code));
    }

    @Transactional
    public OAuthTokenResponse exchangeAuthorizationCode(String code, String clientId, String redirectUri,
                                                        String codeVerifier, String resource) {
        requireText(code, "invalid_grant", "authorization code가 필요합니다.");
        if (code.length() > 512) throw protocol("invalid_grant", "authorization code가 유효하지 않습니다.");
        OAuthAuthorization authorization = authorizationRepository.findByAuthorizationCodeHash(hash(code))
                .orElseThrow(() -> protocol("invalid_grant", "authorization code가 유효하지 않습니다."));
        OffsetDateTime now = now();
        if (!"APPROVED".equals(authorization.getStatus()) || authorization.getConsumedAt() != null
                || authorization.getCodeExpiresAt() == null || authorization.getCodeExpiresAt().isBefore(now)) {
            throw protocol("invalid_grant", "authorization code가 만료되었거나 이미 사용되었습니다.");
        }
        if (!authorization.getClientId().equals(clientId)
                || !authorization.getRedirectUri().equals(redirectUri)
                || !authorization.getResource().equals(resource)) {
            throw protocol("invalid_grant", "authorization 요청 정보가 일치하지 않습니다.");
        }
        if (!isPkceValue(codeVerifier)
                || !constantTimeEquals(authorization.getCodeChallenge(), pkceChallenge(codeVerifier))) {
            throw protocol("invalid_grant", "PKCE code_verifier가 일치하지 않습니다.");
        }
        requireActiveUser(authorization.getUserId());
        authorization.consume(now);
        return issueToken(clientId, authorization.getUserId(), authorization.getScope(), resource, now);
    }

    @Transactional
    public OAuthTokenResponse refresh(String refreshToken, String clientId, String resource) {
        requireText(refreshToken, "invalid_grant", "refresh_token이 필요합니다.");
        if (refreshToken.length() > 512) throw protocol("invalid_grant", "refresh_token이 유효하지 않습니다.");
        OAuthToken token = tokenRepository.findByRefreshTokenHash(hash(refreshToken))
                .orElseThrow(() -> protocol("invalid_grant", "refresh_token이 유효하지 않습니다."));
        OffsetDateTime now = now();
        if (token.getRevokedAt() != null || token.getRefreshExpiresAt().isBefore(now)
                || !token.getClientId().equals(clientId) || !token.getResource().equals(resource)) {
            throw protocol("invalid_grant", "refresh_token이 만료되었거나 요청 정보가 일치하지 않습니다.");
        }
        requireActiveUser(token.getUserId());
        String access = "bmb_oauth_" + randomValue(32);
        String refresh = "bmb_refresh_" + randomValue(32);
        token.rotate(hash(access), hash(refresh), now.plusMinutes(accessTokenMinutes),
                now.plusDays(refreshTokenDays));
        return new OAuthTokenResponse(access, "Bearer", accessTokenMinutes * 60, refresh, token.getScope());
    }

    @Transactional
    public void revoke(String rawToken, String clientId) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 512
                || clientId == null || clientId.isBlank()) return;
        String tokenHash = hash(rawToken);
        tokenRepository.findByAccessTokenHashOrRefreshTokenHash(tokenHash, tokenHash)
                .filter(token -> token.getClientId().equals(clientId))
                .ifPresent(token -> token.revoke(now()));
    }

    @Transactional(readOnly = true)
    public OAuthIntrospectionResponse introspect(String authorizationHeader, String rawToken) {
        verifyInternalToken(authorizationHeader);
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 512) {
            return OAuthIntrospectionResponse.inactive();
        }
        OAuthToken token = tokenRepository.findByAccessTokenHash(hash(rawToken)).orElse(null);
        OffsetDateTime now = now();
        if (token == null || token.getRevokedAt() != null || token.getAccessExpiresAt().isBefore(now)
                || userRepository.findById(token.getUserId()).filter(user -> user.getDeletedAt() == null).isEmpty()) {
            return OAuthIntrospectionResponse.inactive();
        }
        return new OAuthIntrospectionResponse(true, String.valueOf(token.getUserId()), token.getClientId(),
                token.getScope(), token.getAccessExpiresAt().toEpochSecond(), token.getResource());
    }

    private OAuthTokenResponse issueToken(String clientId, Long userId, String scope,
                                          String resource, OffsetDateTime now) {
        String access = "bmb_oauth_" + randomValue(32);
        String refresh = "bmb_refresh_" + randomValue(32);
        tokenRepository.save(new OAuthToken(clientId, userId, scope, resource, hash(access), hash(refresh),
                now.plusMinutes(accessTokenMinutes), now.plusDays(refreshTokenDays), now));
        return new OAuthTokenResponse(access, "Bearer", accessTokenMinutes * 60, refresh, scope);
    }

    private void validateRegistration(OAuthClientRegistrationRequest request) {
        if (request == null || request.clientName() == null || request.clientName().isBlank()
                || request.clientName().trim().length() > 200) {
            throw protocol("invalid_client_metadata", "client_name은 1~200자여야 합니다.");
        }
        if (request.redirectUris() == null || request.redirectUris().isEmpty()
                || request.redirectUris().size() > 10
                || request.redirectUris().stream().anyMatch(uri -> !isSafeRedirectUri(uri))) {
            throw protocol("invalid_redirect_uri", "안전한 redirect_uris가 1~10개 필요합니다.");
        }
        if (new LinkedHashSet<>(request.redirectUris()).size() != request.redirectUris().size()) {
            throw protocol("invalid_redirect_uri", "redirect_uris에 중복 값이 있습니다.");
        }
        if (request.tokenEndpointAuthMethod() != null && !"none".equals(request.tokenEndpointAuthMethod())) {
            throw protocol("invalid_client_metadata", "공개 PKCE client(token_endpoint_auth_method=none)만 지원합니다.");
        }
        if (request.grantTypes() != null
                && !Set.of("authorization_code", "refresh_token").containsAll(request.grantTypes())) {
            throw protocol("invalid_client_metadata", "지원하지 않는 grant_type입니다.");
        }
        if (request.responseTypes() != null
                && !Set.of("code").containsAll(request.responseTypes())) {
            throw protocol("invalid_client_metadata", "지원하지 않는 response_type입니다.");
        }
    }

    private OAuthConnectionResponse toConnection(OAuthToken token, OffsetDateTime now) {
        String clientName = clientRepository.findById(token.getClientId())
                .map(OAuthClient::getClientName)
                .orElse(token.getClientId());
        String status = token.getRevokedAt() != null
                ? "revoked"
                : token.getRefreshExpiresAt().isBefore(now) ? "expired" : "active";
        return new OAuthConnectionResponse(token.getId(), clientName, token.getScope(), status,
                token.getCreatedAt(), token.getRefreshExpiresAt(), token.getRevokedAt());
    }

    private String clientOrigin(String redirectUri) {
        URI uri = URI.create(redirectUri);
        if (uri.getHost() == null) return uri.getScheme() + ":";
        int port = uri.getPort();
        return uri.getScheme() + "://" + uri.getHost() + (port < 0 ? "" : ":" + port);
    }

    private boolean isSafeRedirectUri(String raw) {
        if (raw == null || raw.length() > 1000) return false;
        try {
            URI uri = URI.create(raw);
            if (!uri.isAbsolute() || uri.getFragment() != null || uri.getUserInfo() != null) return false;
            if ("https".equalsIgnoreCase(uri.getScheme())) return uri.getHost() != null;
            if ("http".equalsIgnoreCase(uri.getScheme())) {
                String host = uri.getHost();
                return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host);
            }
            return uri.getScheme().contains(".") && uri.getPath() != null && !uri.getPath().isBlank();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void requireActiveUser(Long userId) {
        if (userId == null || userRepository.findById(userId).filter(user -> user.getDeletedAt() == null).isEmpty()) {
            throw new ApiException(ErrorCode.AUTH_INVALID_TOKEN);
        }
    }

    private void verifyInternalToken(String authorizationHeader) {
        if (internalToken.isBlank()) {
            throw new OAuthProtocolException("server_error", "내부 인증 토큰이 설정되지 않았습니다.",
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
        }
        String expected = "Bearer " + internalToken;
        if (authorizationHeader == null || !constantTimeEquals(expected, authorizationHeader)) {
            throw new OAuthProtocolException("invalid_token", "내부 인증 토큰이 유효하지 않습니다.",
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
    }

    private OAuthProtocolException redirectError(String redirectUri, String state, String error, String description) {
        return new OAuthRedirectException(callback(redirectUri, state,
                "error", error, "error_description", description));
    }

    private String callback(String redirectUri, String state, String... pairs) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(redirectUri);
        for (int i = 0; i < pairs.length; i += 2) builder.queryParam(pairs[i], pairs[i + 1]);
        if (state != null && !state.isBlank()) builder.queryParam("state", state);
        return builder.build().encode().toUriString();
    }

    private String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) return READ_SCOPE;
        return String.join(" ", Arrays.stream(scope.trim().split("\\s+"))
                .distinct().sorted().toList());
    }

    private boolean isPkceValue(String value) {
        return value != null && value.matches("[A-Za-z0-9._~-]{43,128}");
    }

    private String pkceChallenge(String verifier) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(sha256(verifier));
    }

    private String hash(String value) {
        return java.util.HexFormat.of().formatHex(sha256(value));
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    private String randomValue(int bytes) {
        byte[] value = new byte[bytes];
        RANDOM.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    }

    private void requireText(String value, String error, String description) {
        if (value == null || value.isBlank()) throw protocol(error, description);
    }

    private OAuthProtocolException protocol(String error, String description) {
        return new OAuthProtocolException(error, description);
    }

    private String stripTrailingSlash(String value) {
        return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
