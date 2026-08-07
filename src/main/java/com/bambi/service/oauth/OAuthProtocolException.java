package com.bambi.service.oauth;

import org.springframework.http.HttpStatus;

public class OAuthProtocolException extends RuntimeException {

    private final String error;
    private final HttpStatus status;

    OAuthProtocolException(String error, String description) {
        this(error, description, HttpStatus.BAD_REQUEST);
    }

    OAuthProtocolException(String error, String description, HttpStatus status) {
        super(description);
        this.error = error;
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
