package com.bambi.service.follow;

import java.io.Serializable;
import java.util.Objects;

/**
 * {@link Follow} 복합키 (@IdClass). service.follows 의 PK(follower_id, followee_id) 와 대응.
 * JPA 규약상 필드명은 엔티티의 @Id 필드명과 같아야 하고, no-arg 생성자 + equals/hashCode 필수.
 */
public class FollowId implements Serializable {

    private Long followerId;
    private Long followeeId;

    public FollowId() {
    }

    public FollowId(Long followerId, Long followeeId) {
        this.followerId = followerId;
        this.followeeId = followeeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FollowId that)) {
            return false;
        }
        return Objects.equals(followerId, that.followerId)
                && Objects.equals(followeeId, that.followeeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(followerId, followeeId);
    }
}
