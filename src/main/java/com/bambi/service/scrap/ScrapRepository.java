package com.bambi.service.scrap;

import com.bambi.service.card.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScrapRepository extends JpaRepository<Scrap, ScrapId> {

    /**
     * 멱등 스크랩 — 이미 담았으면 조용히 무시(ON CONFLICT DO NOTHING).
     * 낙관적 업데이트 더블탭/재시도에도 유니크 위반 없이 안전.
     */
    @Modifying
    @Query(value = "INSERT INTO service.scraps(user_id, card_id) "
            + "VALUES (:userId, :cardId) ON CONFLICT DO NOTHING", nativeQuery = true)
    int insertIgnore(@Param("userId") Long userId, @Param("cardId") Long cardId);

    /** 스크랩 취소 — 없어도 0건, 멱등. */
    @Modifying
    @Query("delete from Scrap s where s.userId = :userId and s.cardId = :cardId")
    int deleteRelation(@Param("userId") Long userId, @Param("cardId") Long cardId);

    /**
     * 내 스크랩 목록 — 담아둔 카드 중 아직 PUBLIC(살아있음)인 것만 스크랩 최신순으로.
     * 비공개 전환/삭제된 카드는 목록에서 자동 숨김(WHERE 조건). tags 는 Card.interestTags @BatchSize 로 로딩.
     */
    @Query("select c from Card c, Scrap s "
            + "where s.cardId = c.id and s.userId = :userId "
            + "and c.visibility = 'PUBLIC' and c.deletedAt is null "
            + "order by s.createdAt desc")
    List<Card> findMyScrappedPublicCards(@Param("userId") Long userId);
}
