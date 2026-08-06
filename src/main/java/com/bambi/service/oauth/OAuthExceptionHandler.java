package com.bambi.service.oauth;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = {OAuthController.class, OAuthInternalController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OAuthExceptionHandler {

    @ExceptionHandler(OAuthProtocolException.class)
    public ResponseEntity<Map<String, String>> handle(OAuthProtocolException exception) {
        return ResponseEntity.status(exception.getStatus())
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(Map.of(
                        "error", exception.getError(),
                        "error_description", exception.getMessage()));
    }
}
