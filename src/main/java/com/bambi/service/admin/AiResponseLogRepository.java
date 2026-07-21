package com.bambi.service.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiResponseLogRepository extends JpaRepository<AiResponseLog, Long> {

    /** 한 요청의 최신 응답. 없으면 아직 처리 중이라는 뜻. */
    Optional<AiResponseLog> findFirstByRequestIdOrderByCreatedAtDesc(Long requestId);
}
