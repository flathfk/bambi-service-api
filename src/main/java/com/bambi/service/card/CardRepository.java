package com.bambi.service.card;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, Long> {

    /**
     * 내 피드용 카드 목록 (최신순, soft delete 제외).
     * sources 를 @EntityGraph 로 1-shot 로딩해 N+1 을 차단한다.
     */
    @EntityGraph(attributePaths = "sources")
    List<Card> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    /**
     * 카드 단건 조회 (대외 식별자 publicId + 소유자 범위, soft delete 제외).
     * sources 를 함께 로딩(@EntityGraph). 없으면 empty → 컨트롤러가 404.
     * 남의 카드도 존재 노출 없이 empty(404) 로 처리된다.
     */
    @EntityGraph(attributePaths = "sources")
    Optional<Card> findByPublicIdAndUserIdAndDeletedAtIsNull(UUID publicId, Long userId);

    /** 발행 워커의 멱등 Upsert 판단용 — 같은 사용자+external_content_id 카드 존재 여부 */
    boolean existsByUserIdAndExternalContentId(Long userId, String externalContentId);

    // ── SNS(Week2) ─────────────────────────────────────────────

    /**
     * 소유자 검증 없이 대외 식별자(publicId)로 살아있는 카드 조회.
     * 카드 공개설정 변경(소유자 재확인은 서비스에서)·좋아요 대상 해석(PUBLIC 여부 확인)에 쓴다.
     */
    Optional<Card> findByPublicIdAndDeletedAtIsNull(UUID publicId);

    /** 공개 프로필의 공개 카드 수. */
    long countByUserIdAndVisibilityAndDeletedAtIsNull(Long userId, String visibility);

    /**
     * 공개 피드(전체) — PUBLIC 카드를 최신순으로.
     * 컬렉션(sources) fetch join + Pageable 은 LIMIT 을 SQL 로 못 내려 전량 로딩(HHH000104)하므로
     * 여기서는 @EntityGraph 를 쓰지 않는다. 페이지네이션(LIMIT)이 SQL 로 내려가 부분 인덱스
     * idx_cards_public_feed 가 살아나고, sources 는 Card.sources 의 @BatchSize 로 IN 배치 로딩된다.
     */
    @Query("select c from Card c where c.visibility = 'PUBLIC' and c.deletedAt is null "
            + "order by c.createdAt desc")
    List<Card> findPublicFeed(Pageable pageable);

    /** 공개 피드(팔로잉 스코프) — 내가 팔로우하는 작성자의 PUBLIC 카드만. (@BatchSize 로 sources 배치 로딩) */
    @Query("select c from Card c where c.visibility = 'PUBLIC' and c.deletedAt is null "
            + "and c.userId in :authorIds order by c.createdAt desc")
    List<Card> findPublicFeedByAuthors(@Param("authorIds") Collection<Long> authorIds, Pageable pageable);
}
