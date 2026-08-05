package com.bambi.service.comment.dto;

import com.bambi.service.comment.Comment;
import com.bambi.service.user.User;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 댓글 응답 — 내용 + 작성자 요약. 식별자 id 로 삭제 대상 지정.
 * 작성자는 publicId(UUID)로만 노출(순번 id 숨김).
 */
public record CommentResponse(
        Long id,
        String content,
        AuthorResponse author,
        OffsetDateTime createdAt) {

    public record AuthorResponse(UUID publicId, String username, String displayName) {

        static AuthorResponse from(User user) {
            if (user == null) {
                return new AuthorResponse(null, null, null);
            }
            return new AuthorResponse(user.getPublicId(), user.getUsername(), user.getDisplayName());
        }
    }

    public static CommentResponse from(Comment comment, User author) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                AuthorResponse.from(author),
                comment.getCreatedAt());
    }
}
