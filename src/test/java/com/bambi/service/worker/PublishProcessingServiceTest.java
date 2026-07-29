package com.bambi.service.worker;

import com.bambi.service.agent.publish.dto.PublishItem;
import com.bambi.service.card.Card;
import com.bambi.service.card.CardRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link PublishProcessingService} 진짜 upsert 검증 — content_id 만 보고 skip 하던 버그 픽스.
 * 신규 저장 / 더 큰 version 갱신 / 같거나 작은 version skip.
 */
class PublishProcessingServiceTest {

    private final CardRepository cardRepository = mock(CardRepository.class);
    private final PublishProcessingService service = new PublishProcessingService(cardRepository);

    private static PublishItem item(String contentId, int version, String title, String summary) {
        return new PublishItem(contentId, "1", version, "hash-" + version, title, summary, "body",
                List.of(new PublishItem.Citation("src", "https://example.com")));
    }

    @Test
    void 없으면_신규_저장한다() {
        when(cardRepository.findByUserIdAndExternalContentId(1L, "c1")).thenReturn(Optional.empty());

        boolean ok = service.upsert(item("c1", 1, "제목", "요약"));

        assertThat(ok).isTrue();
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void 더_큰_version_이면_내용을_갱신한다() {
        Card existing = Card.fromExternal(1L, "c1", 1, "옛 제목", "옛 요약", null);
        existing.addSource("old", "https://old");
        when(cardRepository.findByUserIdAndExternalContentId(1L, "c1")).thenReturn(Optional.of(existing));

        boolean ok = service.upsert(item("c1", 2, "새 제목", "새 요약"));

        assertThat(ok).isTrue();
        assertThat(existing.getExternalVersion()).isEqualTo(2);
        assertThat(existing.getTitle()).isEqualTo("새 제목");
        assertThat(existing.getSummary()).isEqualTo("새 요약");
        // sources 는 통째 교체 (옛 것 제거 + 새 것 1개)
        assertThat(existing.getSources()).hasSize(1);
        assertThat(existing.getSources().get(0).getUrl()).isEqualTo("https://example.com");
        // 갱신은 dirty checking — 새 row insert(save) 아님
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void 같은_version_이면_skip_한다() {
        Card existing = Card.fromExternal(1L, "c1", 2, "제목", "요약", null);
        when(cardRepository.findByUserIdAndExternalContentId(1L, "c1")).thenReturn(Optional.of(existing));

        boolean ok = service.upsert(item("c1", 2, "덮어쓰기 시도", "덮어쓰기"));

        assertThat(ok).isTrue();
        assertThat(existing.getTitle()).isEqualTo("제목");   // 안 바뀜
        assertThat(existing.getExternalVersion()).isEqualTo(2);
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void 더_작은_version_이면_skip_한다() {
        Card existing = Card.fromExternal(1L, "c1", 5, "최신", "최신요약", null);
        when(cardRepository.findByUserIdAndExternalContentId(1L, "c1")).thenReturn(Optional.of(existing));

        boolean ok = service.upsert(item("c1", 3, "구버전", "구버전요약"));

        assertThat(ok).isTrue();
        assertThat(existing.getTitle()).isEqualTo("최신");   // 구버전이 최신을 못 덮음
        assertThat(existing.getExternalVersion()).isEqualTo(5);
        verify(cardRepository, never()).save(any(Card.class));
    }
}
