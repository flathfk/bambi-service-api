package com.bambi.service.report;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, Long> {

    /** 발행 워커의 멱등 Upsert 판단용 — 같은 사용자+external_content_id 리포트 조회 */
    Optional<Report> findByUserIdAndExternalContentId(Long userId, String externalContentId);

    /**
     * 리포트 단건 조회 (대외 식별자 publicId + 소유자 범위, soft delete 제외).
     * 카드 상세의 본문 진입용. citations 를 함께 로딩(@EntityGraph).
     */
    @EntityGraph(attributePaths = "citations")
    Optional<Report> findByPublicIdAndUserIdAndDeletedAtIsNull(UUID publicId, Long userId);
}
