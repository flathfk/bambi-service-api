package com.bambi.service.agent;

import com.bambi.service.agent.dto.AgentClippingRequest;
import com.bambi.service.agent.dto.AgentAcceptedJob;
import com.bambi.service.agent.dto.AgentUrlSourceRequest;
import com.bambi.service.bookmark.BookmarkSavedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;
import com.bambi.service.wiki.WikiBuildOperationService;

/**
 * 북마크 저장 완료 → agent 위키 원천 처리로 중계.
 *
 * <p>저장 트랜잭션이 커밋된 뒤(AFTER_COMMIT) 실행하고, 중계 실패는 삼켜 저장 자체는 막지 않는다
 * (가입 컨텍스트 동기화와 동일 정책). agent 클리핑 계약이 URL·본문을 모두 요구하므로 분기한다:
 * <ul>
 *   <li>URL + <b>페이지 본문</b> → clippings (웹 클리핑 — agent 는 준 본문을 그대로 쓴다)
 *   <li>URL + <b>짧은 손메모</b> 또는 URL 만 → urls (agent 가 URL 을 직접 읽고, 손메모는 memo 로 함께 받는다)
 *   <li>본문만(URL 없음) → 중계 대상 아님(둘 다 URL 필수) — 건너뜀
 * </ul>
 *
 * <p><b>본문 길이로 갈라야 하는 이유(2026-08-12 확인).</b> clippings 는 <b>준 본문이 페이지 내용이라고 믿고</b>
 * URL 을 다시 읽지 않는다. 그래서 "URL + 키워드 몇 자"로 저장하면 그 몇 자만 위키에 들어가고 페이지 내용은
 * 통째로 버려진다(실사례: 호텔 프로모션 URL + 본문 "도쿄 여행" 5자 → 위키에 "도쿄"만 남음). 운영 데이터상
 * 클리퍼가 보내는 본문은 평균 4,213자, 손으로 적은 메모는 평균 29자로 두 자릿수 넘게 갈리므로
 * {@link #MIN_PAGE_CONTENT_LENGTH} 로 구분한다. 손메모는 버리지 않고 {@code memo} 로 넘겨
 * <b>URL 내용과 사용자가 적은 키워드가 한 자료에 같이</b> 들어가게 한다.
 */
@Component
public class BookmarkClippingRelayListener {

    private static final Logger log = LoggerFactory.getLogger(BookmarkClippingRelayListener.class);

    /**
     * 이 길이 미만이면 "페이지 본문"이 아니라 손으로 적은 메모로 보고, URL 을 agent 가 직접 읽게 한다.
     *
     * <p>운영 데이터 기준(2026-08-12): 클리퍼 본문 평균 4,213자 · 손메모 평균 29자. 200 은 그 사이의
     * 넓은 빈 구간이라 어느 쪽으로도 오분류가 잘 나지 않는다. 짧은 페이지가 urls 로 새더라도 agent 가
     * 원문을 읽으므로 손해가 없고, 반대 방향(긴 메모가 clippings 로)만 URL 을 놓치는 손해가 있다.
     */
    static final int MIN_PAGE_CONTENT_LENGTH = 200;

    private final AgentGateway agentGateway;
    private final WikiBuildOperationService operationService;

    public BookmarkClippingRelayListener(
            AgentGateway agentGateway,
            WikiBuildOperationService operationService) {
        this.agentGateway = agentGateway;
        this.operationService = operationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookmarkSaved(BookmarkSavedEvent event) {
        String sourceEventId = sourceEventId(event);
        boolean hasUrl = StringUtils.hasText(event.url());
        boolean hasPageContent = StringUtils.hasText(event.content())
                && event.content().length() >= MIN_PAGE_CONTENT_LENGTH;

        try {
            if (hasUrl && hasPageContent) {
                AgentAcceptedJob accepted = agentGateway.relayClipping(event.userId(), new AgentClippingRequest(
                        sourceEventId, event.url(), titleOrFallback(event), event.content(), null, java.util.List.of()));
                operationService.register(event.userId(), sourceEventId, accepted);
            } else if (hasUrl) {
                // 본문이 없거나 손메모 수준 — agent 가 URL 을 읽게 하고, 사용자가 적은 건 memo 로 같이 보낸다.
                AgentAcceptedJob accepted = agentGateway.relayUrlSource(event.userId(),
                        new AgentUrlSourceRequest(sourceEventId, event.url(), userMemo(event)));
                operationService.register(event.userId(), sourceEventId, accepted);
            } else {
                // 본문만 있는 메모 — clippings·urls 둘 다 URL 필수라 현재 중계 대상 아님.
                log.debug("[BookmarkRelay] URL 없는 저장 — 위키 중계 건너뜀 (bookmarkId={})", event.bookmarkId());
            }
        } catch (Exception e) {
            // 저장은 이미 커밋됨 — 중계 실패가 사용자 저장을 되돌리지 않는다.
            log.warn("[BookmarkRelay] agent 위키 중계 실패 (bookmarkId={}) — 저장은 유지", event.bookmarkId(), e);
        }
    }

    /**
     * agent 멱등 키 — <b>북마크 ID + 저장 내용 지문</b>.
     *
     * <p><b>ID 만 쓰면 다시 저장해도 agent 가 무시한다(2026-08-13 확인).</b> 같은 URL 로 저장하면 새 행을
     * 만들지 않고 기존 북마크의 제목·본문만 갱신하는데, 키가 {@code bookmark-{id}} 로 고정이라 agent 가
     * "이미 처리한 원천"으로 보고 <b>먼저 끝난 job 을 그대로 돌려준다</b>. 실사례: 같은 호텔 URL 에 키워드를
     * 도쿄 → 상하이 → 발리로 바꿔 세 번 저장했는데 job 은 첫 건 하나뿐이었고(응답 job_id 동일),
     * 상하이·발리는 위키에 영영 들어가지 못해 [AI 가 최근 발견한 관심사]에도 뜨지 않았다.
     *
     * <p>그래서 <b>저장 내용의 해시</b>를 뒤에 붙인다. 내용이 그대로면 지문도 같아 중복 처리를 막는
     * 멱등성이 유지되고(재시도·같은 내용 재저장은 여전히 1건), 내용이 실제로 바뀔 때만 새 키가 되어
     * agent 가 다시 읽는다. {@code updatedAt} 을 쓰지 않는 이유는 {@code @UpdateTimestamp} 가 flush
     * 시점에 채워져, 이벤트를 만드는 시점에는 아직 <b>갱신 전 값</b>일 수 있어서다.
     */
    private static String sourceEventId(BookmarkSavedEvent event) {
        return "bookmark-" + event.bookmarkId() + "-" + fingerprint(event);
    }

    /** 저장 내용(URL·제목·본문) 지문 12자 — 구분자는 본문에 나올 수 없는 NUL 로 둬 경계를 섞지 않는다. */
    private static String fingerprint(BookmarkSavedEvent event) {
        String material = String.join("\0",
                nullToEmpty(event.url()), nullToEmpty(event.title()), nullToEmpty(event.content()));
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(12);
            for (int i = 0; i < 6; i++) {
                hex.append(String.format("%02x", digest[i]));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            // SHA-256 은 JDK 표준 필수 알고리즘이라 도달하지 않는다.
            throw new IllegalStateException("SHA-256 미지원", e);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * urls 경로로 보낼 때 함께 넘길 사용자 메모 — 제목·짧은 본문 중 <b>사용자가 적은 것</b>을 살린다.
     *
     * <p>이게 없으면 "URL + 키워드" 저장에서 키워드가 통째로 사라진다. 제목이 본문에 이미 포함돼 있으면
     * (예: 제목 "도쿄" · 본문 "도쿄 여행") 중복이라 본문만 남긴다. 둘 다 없으면 {@code null} —
     * {@code memo} 는 선택 필드고 NON_NULL 직렬화라 아예 빠진다.
     */
    private String userMemo(BookmarkSavedEvent event) {
        String title = StringUtils.hasText(event.title()) ? event.title().trim() : null;
        String content = StringUtils.hasText(event.content()) ? event.content().trim() : null;
        if (title == null) {
            return content;
        }
        if (content == null) {
            return title;
        }
        return content.contains(title) ? content : title + " " + content;
    }

    /** 클리핑 title 은 필수(비어 있으면 안 됨) — 제목 없으면 대체 문구를 넣는다. */
    private String titleOrFallback(BookmarkSavedEvent event) {
        return StringUtils.hasText(event.title()) ? event.title() : "저장한 자료";
    }
}
