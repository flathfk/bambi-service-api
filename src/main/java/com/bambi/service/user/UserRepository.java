package com.bambi.service.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    /** 살아있는 사용자 id 목록 — 생성 스케줄러가 사용자별 생성 요청을 돌릴 때 쓴다(엔티티 전체 로딩 회피). */
    @Query("select u.id from User u where u.deletedAt is null")
    List<Long> findAllActiveIds();
}
