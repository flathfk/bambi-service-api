package com.bambi.service.oauth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthClientRepository extends JpaRepository<OAuthClient, String> {
}
