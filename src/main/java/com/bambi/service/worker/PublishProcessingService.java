package com.bambi.service.worker;

import com.bambi.service.agent.publish.dto.PublishItem;
import com.bambi.service.card.Card;
import com.bambi.service.card.CardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 발행 항목 1건을 service-db 카드로 멱등 Upsert (§4: content_id + version 키).
 * 항목별 독립 트랜잭션 — 배치 전체를 한 트랜잭션으로 묶지 않는다(부분 성공 허용).
 *
 * <p>진짜 upsert: 같은 (userId, content_id) 카드가
 *  - 없으면 → 신규 저장
 *  - 있고 수신 version 이 더 크면 → 내용 갱신 (agent 가 스냅샷을 갱신 발행한 경우)
 *  - 있고 version 이 같거나 작으면 → skip (재-claim/중복/구버전 도착)
 */
@Service
public class PublishProcessingService {

    private static final Logger log = LoggerFactory.getLogger(PublishProcessingService.class);

    private final CardRepository cardRepository;

    public PublishProcessingService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    /**
     * @return true = 발행 반영됨(신규/갱신) 또는 이미 최신이라 skip. 실패는 예외로 전파.
     */
    @Transactional
    public boolean upsert(PublishItem item) {
        Long userId = item.userIdAsLong();
        Optional<Card> existing = cardRepository.findByUserIdAndExternalContentId(userId, item.contentId());

        if (existing.isPresent()) {
            Card card = existing.get();
            Integer storedVersion = card.getExternalVersion();
            if (!isNewer(item.version(), storedVersion)) {
                // 재-claim/중복/구버전 → 최신본 보존, 덮어쓰지 않음(멱등).
                log.info("[PublishWorker] 이미 최신 skip contentId={} (수신 v{}, 저장 v{})",
                        item.contentId(), item.version(), storedVersion);
                return true;
            }
            card.updateExternal(item.version(), item.title(), item.summary());
            addSources(card, item);   // sources 통째 교체
            log.info("[PublishWorker] 카드 갱신 contentId={} (v{} → v{})",
                    item.contentId(), storedVersion, item.version());
            return true;   // dirty checking 으로 flush
        }

        Card card = Card.fromExternal(
                userId, item.contentId(), item.version(), item.title(), item.summary(), null);
        addSources(card, item);
        try {
            cardRepository.save(card);
        } catch (DataIntegrityViolationException e) {
            // 동시 워커/재시도로 유니크 인덱스 충돌 → 이미 발행된 것으로 간주(멱등).
            log.info("[PublishWorker] 유니크 충돌 → 멱등 처리 contentId={}", item.contentId());
            return true;
        }
        log.info("[PublishWorker] 카드 발행 contentId={}, userId={}, v{}",
                item.contentId(), userId, item.version());
        return true;
    }

    /** 수신 version 이 저장본보다 큰가. version 없으면(=null) 갱신하지 않는다(구현 안전). */
    private boolean isNewer(Integer incoming, Integer stored) {
        if (incoming == null) {
            return false;
        }
        if (stored == null) {
            return true;
        }
        return incoming > stored;
    }

    private void addSources(Card card, PublishItem item) {
        if (item.citations() != null) {
            item.citations().forEach(c -> card.addSource(c.title(), c.url()));
        }
    }
}
