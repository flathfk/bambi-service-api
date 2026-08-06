package com.bambi.service.oauth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OAuthAuthorizationRepository extends JpaRepository<OAuthAuthorization, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from OAuthAuthorization a where a.id = :id")
    Optional<OAuthAuthorization> findLockedById(String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OAuthAuthorization> findByAuthorizationCodeHash(String authorizationCodeHash);
}
