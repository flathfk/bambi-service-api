package com.bambi.service.follow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * 팔로우 관계 (service.follows) — follower_id 가 followee_id 를 팔로우한다.
 * likes 처럼 순수 조인 테이블이라 복합키(@IdClass) + 하드 삭제(언팔 = row 삭제)로 다룬다.
 * 소유자/자기참조 무결성은 FK + CHECK(V4) 와 서비스 레이어가 함께 강제한다.
 */
@Entity
@Table(name = "follows")
@IdClass(FollowId.class)
public class Follow {

    @Id
    @Column(name = "follower_id")
    private Long followerId;

    @Id
    @Column(name = "followee_id")
    private Long followeeId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Follow() {
    }

    public Follow(Long followerId, Long followeeId) {
        this.followerId = followerId;
        this.followeeId = followeeId;
    }

    public Long getFollowerId() {
        return followerId;
    }

    public Long getFolloweeId() {
        return followeeId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
