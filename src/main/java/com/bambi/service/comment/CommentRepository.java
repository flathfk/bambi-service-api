package com.bambi.service.comment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** 카드별 댓글 목록 (오래된 순 = 대화 흐름, soft delete 제외). */
    List<Comment> findByCardIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long cardId);

    /** 삭제 대상 — 내 댓글(작성자)만. 없으면 empty → 404. */
    Optional<Comment> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);
}
