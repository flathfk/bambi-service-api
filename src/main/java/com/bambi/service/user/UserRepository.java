package com.bambi.service.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** 관리자 목록용 — 전체 사용자를 가입 최신순으로. */
    List<User> findAllByOrderByCreatedAtDesc();
}
