package com.bambi.service.briefing;

import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.interest.InterestService;
import com.bambi.service.interest.dto.InterestResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 아침 브리핑용 사용자 선택 관심사 (2026-08-07 저녁 송우·기용 확정).
 *
 * <p>아침 브리핑은 사용자가 미리 고른 관심사 3개로 만들어진다. 여기 저장된 이름이
 * agent 요청의 {@code topics[]} 로 그대로 나간다 — 변환이 없다.
 *
 * <p><b>미선택(빈 목록)은 정상 상태다.</b> 선택 화면을 아직 안 봤거나 선택을 해제한
 * 사용자가 여기 해당한다. 그때 아침 브리핑은 등록 관심사로 폴백한다
 * (agent-api #20 폴백 3단계) — 발행이 멈추지 않는다.
 */
@Service
public class BriefingTopicService {

    /** 아침 브리핑 주제 상한. 2026-08-07 이송우 확정("아침은 3개"). */
    public static final int MAX_TOPICS = 3;

    /** 주제 1개 길이 상한 — agent {@code GenerationRequest.topics} 항목 상한과 맞춘다. */
    public static final int MAX_TOPIC_LENGTH = 500;

    private final BriefingTopicRepository repository;
    private final InterestService interestService;

    public BriefingTopicService(BriefingTopicRepository repository, InterestService interestService) {
        this.repository = repository;
        this.interestService = interestService;
    }

    /**
     * 아침 브리핑에 실제로 보낼 주제 — <b>폴백 3단계</b> (2026-08-08, 황유림 지적으로 확정).
     *
     * <ol>
     *   <li>사용자가 미리 고른 주제</li>
     *   <li>없으면 <b>등록 관심사</b>(온보딩에서 고른 것 + 직접 추가한 것) 최근 {@value #MAX_TOPICS}개</li>
     *   <li>그것도 없으면 빈 목록 → 호출부가 건너뛴다</li>
     * </ol>
     *
     * <p><b>폴백이 없으면 아침 브리핑이 전면 중단된다.</b> 선택 화면이 나오기 전에는 고른 사람이
     * 아무도 없고, 나온 뒤에도 설정을 안 건드린 사용자는 계속 못 받는다.
     *
     * <p><b>2단계에 Wiki 태그 상위 N개를 쓰지 않는다.</b> 저장한 글에서 뽑힌 파편이 상위를
     * 차지하기 때문이다(agent-api #21 — 폭염 기사 1건으로 관심사 상위가 `서울`·`온열질환`·
     * `질병관리청`이 되고 아침 브리핑이 `서울` 로 나갔다). 아침은 사용자가 결과를 검토하지 않고
     * 그냥 받는 경로라 파편이 가장 위험한 자리다. 등록 관심사는 사용자가 직접 고른 값이라
     * 파편이 섞이지 않는다. (08-08 배포된 파편 필터는 기존 Wiki 에 아직 적용되지 않아
     * 재빌드 전까지 이 판단은 그대로 유효하다.)
     */
    @Transactional(readOnly = true)
    public List<String> resolveForMorningBriefing(Long userId) {
        List<String> selected = get(userId);
        if (!selected.isEmpty()) {
            return selected;
        }
        return interestService.list(userId).stream()
                .map(InterestResponse::name)
                .filter(name -> name != null && !name.isBlank())
                .limit(MAX_TOPICS)
                .toList();
    }

    /** 내 선택값 — 미선택이면 빈 목록(404 아님). 화면 진입 시 이 값으로 선택 상태를 복구한다. */
    @Transactional(readOnly = true)
    public List<String> get(Long userId) {
        return repository.findByUserIdOrderByPositionAsc(userId).stream()
                .map(BriefingTopic::getTopic)
                .toList();
    }

    /**
     * 선택값 전체 교체. 빈 목록은 "선택 해제"로 정상 처리한다(폴백으로 돌아간다).
     *
     * <p><b>개수 초과는 조용히 자르지 않고 거절한다.</b> 여기서 앞 3개만 남기면 프론트가
     * 4개를 보낸 실수를 영영 모른 채 사용자 선택 하나가 사라진다. 몇 개를 보낼지는 호출부가 정한다
     * (같은 이유로 {@code WikiTagsResponse.topTags} 도 안에서 개수를 깎지 않는다).
     *
     * <p><b>이름이 현재 Wiki 관심사에 있는지는 검증하지 않는다.</b> Wiki 는 계속 재계산돼서
     * 어제 있던 이름이 오늘 없을 수 있는데, 그때마다 거절하면 사용자 선택이 주기적으로 튕긴다.
     * 그리고 {@code topics} 는 결국 검색어라 Wiki 에 없는 이름이어도 리포트는 정상 생성된다.
     */
    @Transactional
    public List<String> replace(Long userId, List<String> requested) {
        List<String> normalized = normalize(requested);

        repository.deleteByUserId(userId);
        // delete 와 insert 가 같은 트랜잭션이라 (user_id, position) UNIQUE 가 순서상 충돌할 수 있다.
        // flush 로 delete 를 먼저 내보낸 뒤 insert 한다.
        repository.flush();

        List<BriefingTopic> rows = new ArrayList<>(normalized.size());
        for (int i = 0; i < normalized.size(); i++) {
            rows.add(new BriefingTopic(userId, i, normalized.get(i)));
        }
        repository.saveAll(rows);
        return normalized;
    }

    /**
     * 공백 제거 → 빈 항목 제외 → 중복 합침 → 개수 확인. 순서는 처음 등장한 순서를 유지한다
     * (agent 계약상 {@code topics} 순서 = 리포트 섹션 순서라 임의로 흔들면 안 된다).
     *
     * <p><b>개수는 정리한 뒤에 센다.</b> 먼저 세면 빈 문자열·중복 같은 노이즈가 상한에 포함돼,
     * 사용자가 실제로는 3개 이하를 골랐는데도 거절된다. 정리 후에 세면 노이즈는 통과시키면서
     * <b>서로 다른 주제를 4개 보낸 진짜 실수는 그대로 잡힌다</b> — 중복을 합쳐도 4개는 4개다.
     */
    private List<String> normalize(List<String> requested) {
        if (requested == null || requested.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String raw : requested) {
            if (raw == null) {
                continue;
            }
            String topic = raw.strip();
            if (topic.isEmpty()) {
                continue;
            }
            if (topic.length() > MAX_TOPIC_LENGTH) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR,
                        "주제 하나는 " + MAX_TOPIC_LENGTH + "자를 넘을 수 없습니다.");
            }
            unique.add(topic);
        }
        if (unique.size() > MAX_TOPICS) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "아침 브리핑 주제는 최대 " + MAX_TOPICS + "개입니다.");
        }
        return List.copyOf(unique);
    }
}
