package com.bambi.service.briefing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * 아침 브리핑용 사용자 선택 관심사 1건 (service.user_briefing_topics).
 *
 * <p><b>이름으로 저장한다 — agent {@code interest_id}(UUID)로 저장하면 안 된다.</b>
 * 관심 Profile 은 재계산할 때마다 기존 active 를 retired 로 내리고 새 Profile 을 만들며,
 * {@code user_interests} 가 {@code profile_id} 에 CASCADE 로 묶여 있어 재계산 때마다 모든
 * {@code interest_id} 가 새로 발급된다. UUID 로 저장하면 사용자 선택이 다음 재계산에
 * retired 를 가리켜 409 {@code ACTIVE_INTEREST_REQUIRED} 로 조용히 죽는다.
 *
 * <p>{@code position} 은 0부터. agent 계약상 <b>{@code topics} 순서가 곧 리포트 섹션 순서</b>라
 * 순서를 보존해야 한다.
 */
@Entity
@Table(name = "user_briefing_topics")
public class BriefingTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false, updatable = false)
    private int position;

    @Column(nullable = false, length = 500, updatable = false)
    private String topic;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected BriefingTopic() {
    }

    public BriefingTopic(Long userId, int position, String topic) {
        this.userId = userId;
        this.position = position;
        this.topic = topic;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public int getPosition() {
        return position;
    }

    public String getTopic() {
        return topic;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
