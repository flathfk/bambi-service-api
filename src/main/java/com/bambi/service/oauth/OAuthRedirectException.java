package com.bambi.service.oauth;

final class OAuthRedirectException extends OAuthProtocolException {

    private final String redirectUrl;

    OAuthRedirectException(String redirectUrl) {
        super("invalid_request", "OAuth 요청을 client callback으로 돌려보냅니다.");
        this.redirectUrl = redirectUrl;
    }

    String getRedirectUrl() {
        return redirectUrl;
    }
}
