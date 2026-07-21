package com.bambi.service.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiRequestLogRepository extends JpaRepository<AiRequestLog, Long> {

    /** 관리자 로그 화면용 — 요청을 최신순으로. */
    List<AiRequestLog> findAllByOrderByCreatedAtDesc();
}
