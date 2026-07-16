package com.bambi.service.feed;

import com.bambi.service.card.CardRepository;
import com.bambi.service.card.dto.CardResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 카드 피드 (P0) — 내 카드를 최신순으로.
 * P0 피드는 "내 카드 전부"와 동치라 cards 를 직접 조회한다.
 * 공개 피드/RSS 매칭 카드가 들어오는 P1 에서 feed_items 테이블 기반으로 확장한다.
 */
@Service
public class FeedService {

    private final CardRepository cardRepository;

    public FeedService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Transactional(readOnly = true)
    public List<CardResponse> myFeed(Long userId) {
        return cardRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId).stream()
                .map(CardResponse::from)
                .toList();
    }
}
