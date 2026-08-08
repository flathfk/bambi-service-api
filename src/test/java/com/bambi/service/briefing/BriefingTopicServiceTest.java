package com.bambi.service.briefing;

import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link BriefingTopicService} — 아침 브리핑 주제 선택 저장/조회.
 *
 * <p>여기서 지키는 것은 두 가지다. <b>순서</b>(agent 계약상 topics 순서 = 리포트 섹션 순서)와
 * <b>개수 초과를 조용히 자르지 않는 것</b>(프론트 실수를 숨기면 사용자 선택이 소리 없이 사라진다).
 */
class BriefingTopicServiceTest {

    private final BriefingTopicRepository repository = mock(BriefingTopicRepository.class);
    private final BriefingTopicService service = new BriefingTopicService(repository);

    @Test
    void 선택값을_position_순서대로_이름만_돌려준다() {
        when(repository.findByUserIdOrderByPositionAsc(1L)).thenReturn(List.of(
                new BriefingTopic(1L, 0, "폭염"),
                new BriefingTopic(1L, 1, "퇴근"),
                new BriefingTopic(1L, 2, "웹툰·애니")));

        assertThat(service.get(1L)).containsExactly("폭염", "퇴근", "웹툰·애니");
    }

    @Test
    void 한번도_저장한_적_없으면_빈_목록이다() {
        when(repository.findByUserIdOrderByPositionAsc(1L)).thenReturn(List.of());

        // 404 가 아니라 빈 목록이다 — 미선택은 정상 상태고 아침은 폴백으로 발행된다.
        assertThat(service.get(1L)).isEmpty();
    }

    @Test
    void 저장하면_기존_선택을_지우고_0부터_순서대로_넣는다() {
        List<BriefingTopic> saved = replaceAndCapture(1L, List.of("폭염", "퇴근"));

        verify(repository).deleteByUserId(1L);
        assertThat(saved).extracting(BriefingTopic::getPosition).containsExactly(0, 1);
        assertThat(saved).extracting(BriefingTopic::getTopic).containsExactly("폭염", "퇴근");
    }

    @Test
    void 상한을_넘기면_조용히_자르지_않고_거절한다() {
        // 앞 3개만 남기면 프론트가 4개를 보낸 실수를 아무도 모른 채 선택 하나가 사라진다.
        assertThatThrownBy(() -> service.replace(1L, List.of("폭염", "퇴근", "웹툰", "여분")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        verify(repository, never()).deleteByUserId(1L);
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void 공백을_다듬고_빈_항목과_중복을_걸러내며_처음_등장_순서를_지킨다() {
        List<BriefingTopic> saved = replaceAndCapture(1L, java.util.Arrays.asList(
                "  폭염  ", "", "폭염", null));

        // 순서를 임의로 흔들면 리포트 섹션 순서가 바뀐다 — 처음 등장 순서를 유지한다.
        assertThat(saved).extracting(BriefingTopic::getTopic).containsExactly("폭염");
        assertThat(saved).extracting(BriefingTopic::getPosition).containsExactly(0);
    }

    @Test
    void 빈_항목과_중복은_개수_상한에_포함되지_않는다() {
        // 원본은 5개지만 사용자가 실제로 고른 건 3개다. 정리 전에 세면 여기서 억울하게 거절된다.
        List<BriefingTopic> saved = replaceAndCapture(1L, java.util.Arrays.asList(
                "폭염", "", "퇴근", "폭염", "웹툰·애니"));

        assertThat(saved).extracting(BriefingTopic::getTopic)
                .containsExactly("폭염", "퇴근", "웹툰·애니");
    }

    @Test
    void 중복을_합쳐도_서로_다른_주제가_넷이면_거절한다() {
        // 정리 후에 세도 진짜 실수는 잡힌다 — 중복을 합쳐도 서로 다른 4개는 4개다.
        assertThatThrownBy(() -> service.replace(1L,
                List.of("폭염", "폭염", "퇴근", "웹툰", "여분")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        verify(repository, never()).deleteByUserId(1L);
    }

    @Test
    void 빈_목록은_선택_해제로_정상_저장된다() {
        List<String> result = service.replace(1L, List.of());

        assertThat(result).isEmpty();
        verify(repository).deleteByUserId(1L);
    }

    @Test
    void null_목록도_선택_해제로_처리한다() {
        // 프론트가 필드를 생략해 null 이 와도 500 이 나면 안 된다.
        assertThat(service.replace(1L, null)).isEmpty();
        verify(repository).deleteByUserId(1L);
    }

    @Test
    void 주제_하나가_길이_상한을_넘으면_거절한다() {
        String tooLong = String.join("", Collections.nCopies(BriefingTopicService.MAX_TOPIC_LENGTH + 1, "가"));

        assertThatThrownBy(() -> service.replace(1L, List.of(tooLong)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void 저장_응답은_정규화된_결과다() {
        // 프론트는 이 응답으로 화면 상태를 갱신하므로 보낸 값이 아니라 저장된 값이어야 한다.
        assertThat(service.replace(1L, java.util.Arrays.asList("  폭염 ", "퇴근")))
                .containsExactly("폭염", "퇴근");
    }

    @SuppressWarnings("unchecked")
    private List<BriefingTopic> replaceAndCapture(Long userId, List<String> requested) {
        service.replace(userId, requested);
        ArgumentCaptor<List<BriefingTopic>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        return captor.getValue();
    }
}
