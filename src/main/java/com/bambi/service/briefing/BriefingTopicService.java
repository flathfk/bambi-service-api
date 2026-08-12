package com.bambi.service.briefing;

import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.wiki.AgentWikiClient;
import com.bambi.service.wiki.dto.BriefingTopicsSelection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Supplier;

/**
 * 아침 브리핑 주제 결정 (2026-08-11 (B) 확정).
 *
 * <p>주제는 <b>agent 가 개인 Wiki 맥락을 읽어 고른다.</b> 고른 이름이 agent 요청의
 * {@code topics[]} 로 그대로 나간다 — 변환이 없다.
 *
 * <p><b>사용자 선택 저장소({@link #get}·{@link #replace}, {@code user_briefing_topics})는
 * 호출부 없이 남아 있다.</b> 08-11 에 아침 주제를 사용자가 고르지 않기로 확정하면서 선택 화면이
 * 제거됐다(service-web #74). 되살릴 가능성이 있어 지우지 않았고, 정리 여부는 제출 후 결정한다.
 */
@Service
public class BriefingTopicService {

    /** 아침 브리핑 주제 상한. 2026-08-07 이송우 확정("아침은 3개"). */
    public static final int MAX_TOPICS = 3;

    /** 주제 1개 길이 상한 — agent {@code GenerationRequest.topics} 항목 상한과 맞춘다. */
    public static final int MAX_TOPIC_LENGTH = 500;

    private static final Logger log = LoggerFactory.getLogger(BriefingTopicService.class);

    private final BriefingTopicRepository repository;
    private final AgentWikiClient wikiClient;

    public BriefingTopicService(BriefingTopicRepository repository,
                                AgentWikiClient wikiClient) {
        this.repository = repository;
        this.wikiClient = wikiClient;
    }

    /**
     * 아침 브리핑에 실제로 보낼 주제는 Agent가 개인 LLM Wiki에서 고른 결과만 사용한다.
     *
     * <p><b>1단계는 Wiki 태그 점수 상위 N개가 아니다.</b> 연결 수 상위를 그대로 쓰면 도구·출처가
     * 주제가 된다(agent-api #21 — 폭염 기사 1건으로 관심사 상위가 `서울`·`온열질환`·`질병관리청`이
     * 되고 아침 브리핑이 `서울` 로 나갔다. 실측 `DBeaver Community` 1.00). agent 가 후보를 넓게
     * 받아 맥락을 읽고 고르고, 민감 주제(자살·자해·재난 사망·정치인 실명)를 선정 단계에서 뺀다.
     *
     * <p>Snapshot 미준비와 준비 완료 후 빈 결과를 구분한다. Agent 장애는 숨기지 않고 호출부로
     * 전달해 Outbox가 재시도하게 한다.
     */
    @Transactional(readOnly = true)
    public MorningBriefingTopics resolveForMorningBriefing(Long userId) {
        return selectFromWikiContext(userId);
    }

    /** 지정 브리핑 날짜에 준비된 개인 Wiki 주제와 준비 상태를 읽는다. */
    @Transactional(readOnly = true)
    public MorningBriefingTopics resolveForMorningBriefing(Long userId, LocalDate briefingDate) {
        return selectFromWikiContext(userId, briefingDate);
    }

    /**
     * Agent가 저장한 Wiki 선정 결과와 준비 상태를 읽는다.
     *
     * <p>선정 근거({@code reason})를 로그에 남긴다. 결과만 봐서는 왜 그 주제가 뽑혔는지 알 수 없어서,
     * "아침에 이상한 주제가 나갔다"는 신고가 들어왔을 때 이 줄이 유일한 단서다.
     *
     * <p>REPORT-022부터 새벽 준비는 별도 비동기 Job이 담당하고, 이 메서드는 준비된 Snapshot을
     * 읽는 경계로만 남는다.
     */
    public MorningBriefingTopics selectFromWikiContext(Long userId) {
        return selectFromWikiContext(
                userId, () -> wikiClient.getBriefingTopics(userId, MAX_TOPICS));
    }

    /** 지정 날짜 Snapshot의 Agent 선정 결과를 읽는다. */
    public MorningBriefingTopics selectFromWikiContext(Long userId, LocalDate briefingDate) {
        return selectFromWikiContext(
                userId,
                () -> wikiClient.getBriefingTopics(userId, briefingDate, MAX_TOPICS));
    }

    private MorningBriefingTopics selectFromWikiContext(
            Long userId,
            Supplier<BriefingTopicsSelection> selectionLoader) {
        BriefingTopicsSelection selection = selectionLoader.get();
        if (selection == null) {
            throw new IllegalStateException("Agent 아침 브리핑 주제 응답이 없습니다.");
        }
        if (!selection.isPrepared()) {
            log.info("[BriefingTopic] agent 준비 미완료 userId={}", userId);
            return MorningBriefingTopics.notPrepared();
        }
        List<String> topics = selection.normalizedTopics();
        if (topics.isEmpty()) {
            return MorningBriefingTopics.ready(List.of());
        }
        // 개수는 agent 계약(limit)이 이미 지키지만, 계약이 바뀌어도 요청이 커지지 않게 여기서도 자른다.
        List<String> limited = topics.size() > MAX_TOPICS
                ? List.copyOf(topics.subList(0, MAX_TOPICS))
                : topics;
        log.info("[BriefingTopic] agent 선정 userId={}, topics={}, 후보={}, 근거={}",
                userId, limited, selection.candidateCount(), selection.reasonOrEmpty());
        return MorningBriefingTopics.ready(limited);
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
