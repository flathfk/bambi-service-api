package com.bambi.service.oauth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface OAuthTokenRepository extends JpaRepository<OAuthToken, UUID> {

    Optional<OAuthToken> findByAccessTokenHash(String accessTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OAuthToken> findByRefreshTokenHash(String refreshTokenHash);

    Optional<OAuthToken> findByAccessTokenHashOrRefreshTokenHash(String accessTokenHash, String refreshTokenHash);

    List<OAuthToken> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<OAuthToken> findByIdAndUserId(UUID id, Long userId);
}
