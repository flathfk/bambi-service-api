package com.bambi.service.agent.publish.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 테스트용 {@link PublishItem} 조립기.
 *
 * <p>발행 계약은 필드가 18개인 record 다(agent 가 단계적으로 늘려온 결과). 위치 인자로 만들면
 * {@code List.of(), List.of(), null, null, null, null, null, null, true} 처럼 늘어져서
 * <b>그 테스트가 무엇을 검증하는지가 인자 나열에 묻힌다.</b> 그래서 기본값을 두고
 * <b>검증에 필요한 필드만</b> 지정하게 한다.
 *
 * <p>기본값은 "가장 단순한 정상 스냅샷"이다 — 단계적 롤아웃 필드(report_type·taxonomy·
 * cover_image 등)는 전부 미도착(null / false) 상태다. 관용 파싱 경로가 기본이라는 뜻이고,
 * 값이 온 경우를 검증하려면 그 필드만 명시한다.
 */
public final class PublishItemFixture {

    private String contentId = "c1";
    private String userId = "1";
    private Integer version = 1;
    private String snapshotHash = "hash-1";
    private String title = "제목";
    private String summary = "요약";
    private String body = "본문";
    private List<PublishItem.Citation> citations = List.of();
    private List<String> tags = List.of();
    private List<String> contentTags = List.of();
    private String reportType;
    private String requestIdempotencyKey;
    private String generationTopic;
    private OffsetDateTime createdAt;
    private List<String> taxonomyTopicIds;
    private String taxonomyVersion;
    private PublishItem.CoverImage coverImage;
    private boolean changeHistoryEnabled;

    private PublishItemFixture() {
    }

    /** 기본값 스냅샷. */
    public static PublishItemFixture item() {
        return new PublishItemFixture();
    }

    /** 버전 게이트(신규/갱신/skip) 검증에서 가장 많이 쓰는 조합. */
    public static PublishItemFixture item(String contentId, int version) {
        return new PublishItemFixture()
                .contentId(contentId)
                .version(version)
                .snapshotHash("hash-" + version)
                .body("본문-" + version);
    }

    public PublishItemFixture contentId(String value) {
        this.contentId = value;
        return this;
    }

    public PublishItemFixture userId(String value) {
        this.userId = value;
        return this;
    }

    public PublishItemFixture version(Integer value) {
        this.version = value;
        return this;
    }

    public PublishItemFixture snapshotHash(String value) {
        this.snapshotHash = value;
        return this;
    }

    public PublishItemFixture title(String value) {
        this.title = value;
        return this;
    }

    public PublishItemFixture summary(String value) {
        this.summary = value;
        return this;
    }

    public PublishItemFixture body(String value) {
        this.body = value;
        return this;
    }

    public PublishItemFixture citations(PublishItem.Citation... values) {
        this.citations = List.of(values);
        return this;
    }

    /** 생성 topic 에코. {@code contentTags} 가 비었을 때의 폴백 경로를 검증할 때 쓴다. */
    public PublishItemFixture tags(String... values) {
        this.tags = List.of(values);
        return this;
    }

    /** 리포트 내용 기반 태그 — 카드 노출용. {@code tags} 보다 우선한다. */
    public PublishItemFixture contentTags(String... values) {
        this.contentTags = List.of(values);
        return this;
    }

    /** 미도착 상태(null)를 명시하고 싶을 때. 기본값은 빈 목록이다. */
    public PublishItemFixture noContentTags() {
        this.contentTags = null;
        return this;
    }

    public PublishItemFixture reportType(String value) {
        this.reportType = value;
        return this;
    }

    public PublishItemFixture requestIdempotencyKey(String value) {
        this.requestIdempotencyKey = value;
        return this;
    }

    public PublishItemFixture generationTopic(String value) {
        this.generationTopic = value;
        return this;
    }

    public PublishItemFixture createdAt(OffsetDateTime value) {
        this.createdAt = value;
        return this;
    }

    public PublishItemFixture taxonomy(String version, List<String> topicIds) {
        this.taxonomyVersion = version;
        this.taxonomyTopicIds = topicIds;
        return this;
    }

    public PublishItemFixture coverImage(PublishItem.CoverImage value) {
        this.coverImage = value;
        return this;
    }

    /** body 가 변경점(Delta) 4단 폼임을 알리는 신호. */
    public PublishItemFixture changeHistoryEnabled(boolean value) {
        this.changeHistoryEnabled = value;
        return this;
    }

    public PublishItem build() {
        return new PublishItem(
                contentId, userId, version, snapshotHash, title, summary, body,
                citations, tags, contentTags, reportType, requestIdempotencyKey,
                generationTopic, createdAt, taxonomyTopicIds, taxonomyVersion,
                coverImage, changeHistoryEnabled);
    }
}
