package com.bambi.service.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** 관리자 목록용 — 전체 사용자를 가입 최신순으로. */
    List<User> findAllByOrderByCreatedAtDesc();

    /** 대외 식별자(public_id)로 살아있는 사용자 조회 — SNS 팔로우/프로필 진입점. */
    Optional<User> findByPublicIdAndDeletedAtIsNull(UUID publicId);
}
