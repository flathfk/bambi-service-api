package com.bambi.service.worker;

import com.bambi.service.agent.publish.dto.PublishItem;
import com.bambi.service.card.Card;
import com.bambi.service.card.CardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 발행 항목 1건을 service-db 카드로 멱등 Upsert (§4: content_id + version 키).
 * 항목별 독립 트랜잭션 — 배치 전체를 한 트랜잭션으로 묶지 않는다(부분 성공 허용).
 */
@Service
public class PublishProcessingService {

    private static final Logger log = LoggerFactory.getLogger(PublishProcessingService.class);

    private final CardRepository cardRepository;

    public PublishProcessingService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    /**
     * @return true = 발행 완료(신규 저장 or 이미 존재), false 없음(실패는 예외로 전파)
     */
    @Transactional
    public boolean upsert(PublishItem item) {
        // 멱등: 같은 사용자+content_id 카드가 이미 있으면 재-claim/재-ack 이므로 skip.
        Long userId = item.userIdAsLong();
        if (cardRepository.existsByUserIdAndExternalContentId(userId, item.contentId())) {
            log.info("[PublishWorker] 이미 발행됨 skip contentId={}", item.contentId());
            return true;
        }
        Card card = Card.fromExternal(
                userId, item.contentId(), item.version(),
                item.title(), item.summary(), null);
        if (item.citations() != null) {
            item.citations().forEach(c -> card.addSource(c.title(), c.url()));
        }
        try {
            cardRepository.save(card);
        } catch (DataIntegrityViolationException e) {
            // 동시 워커/재시도로 유니크 인덱스 충돌 → 이미 발행된 것으로 간주(멱등).
            log.info("[PublishWorker] 유니크 충돌 → 멱등 처리 contentId={}", item.contentId());
            return true;
        }
        log.info("[PublishWorker] 카드 발행 contentId={}, userId={}", item.contentId(), userId);
        return true;
    }
}
