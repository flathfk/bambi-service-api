package com.bambi.service.card;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long> {

    /**
     * 내 피드용 카드 목록 (최신순, soft delete 제외).
     * sources 를 @EntityGraph 로 1-shot 로딩해 N+1 을 차단한다.
     */
    @EntityGraph(attributePaths = "sources")
    List<Card> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);
}
