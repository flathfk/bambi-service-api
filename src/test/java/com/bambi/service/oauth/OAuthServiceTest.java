package com.bambi.service.oauth;

import com.bambi.service.oauth.dto.OAuthClientRegistrationRequest;
import com.bambi.service.oauth.dto.OAuthIntrospectionResponse;
import com.bambi.service.oauth.dto.OAuthTokenResponse;
import com.bambi.service.user.User;
import com.bambi.service.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuthServiceTest {

    private final OAuthClientRepository clients = mock(OAuthClientRepository.class);
    private final OAuthAuthorizationRepository authorizations = mock(OAuthAuthorizationRepository.class);
    private final OAuthTokenRepository tokens = mock(OAuthTokenRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final OAuthService service = new OAuthService(
            clients, authorizations, tokens, users,
            "https://bambi.test", "https://bambi.test/oauth/authorize",
            "https://bambi.test/mcp", "internal-secret", 10, 5, 60, 30);

    @Test
    void authorizationCodePkceIssuesAndIntrospectsOpaqueAccessToken() throws Exception {
        User user = mock(User.class);
        when(user.getDeletedAt()).thenReturn(null);
        when(users.findById(42L)).thenReturn(Optional.of(user));
        when(clients.save(any(OAuthClient.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authorizations.save(any(OAuthAuthorization.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokens.save(any(OAuthToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var registered = service.register(new OAuthClientRegistrationRequest(
                "ChatGPT", List.of("https://chatgpt.com/aip/callback"), "none",
                List.of("authorization_code", "refresh_token"), List.of("code")));
        ArgumentCaptor<OAuthClient> clientCaptor = ArgumentCaptor.forClass(OAuthClient.class);
        org.mockito.Mockito.verify(clients).save(clientCaptor.capture());
        when(clients.findById(registered.clientId())).thenReturn(Optional.of(clientCaptor.getValue()));

        String verifier = "a".repeat(64);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.UTF_8)));
        service.beginAuthorization("code", registered.clientId(),
                "https://chatgpt.com/aip/callback", "wiki:write wiki:read", "state-value",
                challenge, "S256", "https://bambi.test/mcp");

        ArgumentCaptor<OAuthAuthorization> authorizationCaptor =
                ArgumentCaptor.forClass(OAuthAuthorization.class);
        org.mockito.Mockito.verify(authorizations).save(authorizationCaptor.capture());
        OAuthAuthorization authorization = authorizationCaptor.getValue();
        when(authorizations.findLockedById(authorization.getId())).thenReturn(Optional.of(authorization));

        String redirect = service.decide(42L, authorization.getId(), true).redirectUrl();
        String code = queryParam(redirect, "code");
        assertThat(queryParam(redirect, "state")).isEqualTo("state-value");
        when(authorizations.findByAuthorizationCodeHash(anyString())).thenReturn(Optional.of(authorization));

        OAuthTokenResponse issued = service.exchangeAuthorizationCode(
                code, registered.clientId(), "https://chatgpt.com/aip/callback",
                verifier, "https://bambi.test/mcp");
        assertThat(issued.accessToken()).startsWith("bmb_oauth_");
        assertThat(issued.refreshToken()).startsWith("bmb_refresh_");
        assertThat(issued.scope()).isEqualTo("wiki:read wiki:write");

        ArgumentCaptor<OAuthToken> tokenCaptor = ArgumentCaptor.forClass(OAuthToken.class);
        org.mockito.Mockito.verify(tokens).save(tokenCaptor.capture());
        when(tokens.findByAccessTokenHash(anyString())).thenReturn(Optional.of(tokenCaptor.getValue()));
        OAuthIntrospectionResponse introspected =
                service.introspect("Bearer internal-secret", issued.accessToken());

        assertThat(introspected.active()).isTrue();
        assertThat(introspected.sub()).isEqualTo("42");
        assertThat(introspected.clientId()).isEqualTo(registered.clientId());
        assertThat(introspected.scope()).isEqualTo("wiki:read wiki:write");
        assertThat(introspected.aud()).isEqualTo("https://bambi.test/mcp");

        OAuthToken storedToken = tokenCaptor.getValue();
        when(tokens.findByIdAndUserId(storedToken.getId(), 42L)).thenReturn(Optional.of(storedToken));
        service.revokeConnection(42L, storedToken.getId());
        assertThat(service.introspect("Bearer internal-secret", issued.accessToken()).active()).isFalse();
    }

    @Test
    void registrationRejectsRemotePlainHttpRedirect() {
        assertThatThrownBy(() -> service.register(new OAuthClientRegistrationRequest(
                "Unsafe", List.of("http://attacker.test/callback"), "none", null, null)))
                .isInstanceOf(OAuthProtocolException.class)
                .extracting(exception -> ((OAuthProtocolException) exception).getError())
                .isEqualTo("invalid_redirect_uri");
    }

    @Test
    void metadataAdvertisesPublicPkceAndDynamicRegistration() {
        assertThat(service.authorizationServerMetadata())
                .containsEntry("issuer", "https://bambi.test/")
                .containsEntry("registration_endpoint", "https://bambi.test/api/oauth/register")
                .containsEntry("token_endpoint_auth_methods_supported", List.of("none"))
                .containsEntry("code_challenge_methods_supported", List.of("S256"))
                .containsEntry("scopes_supported", List.of("wiki:read", "wiki:write"));
    }

    @Test
    void authorizationKeepsReadOnlyCompatibilityAndRejectsWriteOnlyScope() {
        OAuthClient client = new OAuthClient(
                "bmb_client_test",
                "Test Client",
                java.util.Set.of("https://client.test/callback"),
                java.time.OffsetDateTime.now()
        );
        when(clients.findById("bmb_client_test")).thenReturn(Optional.of(client));
        when(authorizations.save(any(OAuthAuthorization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        String challenge = "a".repeat(43);

        service.beginAuthorization(
                "code", "bmb_client_test", "https://client.test/callback", "wiki:read",
                "read-state", challenge, "S256", "https://bambi.test/mcp"
        );
        ArgumentCaptor<OAuthAuthorization> authorizationCaptor =
                ArgumentCaptor.forClass(OAuthAuthorization.class);
        org.mockito.Mockito.verify(authorizations).save(authorizationCaptor.capture());
        assertThat(authorizationCaptor.getValue().getScope()).isEqualTo("wiki:read");

        assertThatThrownBy(() -> service.beginAuthorization(
                "code", "bmb_client_test", "https://client.test/callback", "wiki:write",
                "write-state", challenge, "S256", "https://bambi.test/mcp"
        ))
                .isInstanceOf(OAuthRedirectException.class)
                .satisfies(exception -> assertThat(
                        ((OAuthRedirectException) exception).getRedirectUrl()
                ).contains("error=invalid_scope"));
    }

    private String queryParam(String url, String name) {
        String query = URI.create(url).getRawQuery();
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (URLDecoder.decode(parts[0], StandardCharsets.UTF_8).equals(name)) {
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("missing query parameter: " + name);
    }
}
