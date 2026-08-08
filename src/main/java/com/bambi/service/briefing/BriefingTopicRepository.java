package com.bambi.service.briefing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BriefingTopicRepository extends JpaRepository<BriefingTopic, Long> {

    /** 선택값 조회 — agent {@code topics[]} 순서가 곧 리포트 섹션 순서라 position 오름차순으로 읽는다. */
    List<BriefingTopic> findByUserIdOrderByPositionAsc(Long userId);

    /** 전체 교체(PUT) 전에 기존 선택을 비운다. position UNIQUE 충돌을 피하려면 insert 전에 지워야 한다. */
    void deleteByUserId(Long userId);
}
