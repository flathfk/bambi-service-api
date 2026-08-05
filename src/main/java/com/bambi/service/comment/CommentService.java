package com.bambi.service.comment;

import com.bambi.service.card.Card;
import com.bambi.service.card.CardRepository;
import com.bambi.service.comment.dto.CommentRequest;
import com.bambi.service.comment.dto.CommentResponse;
import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.user.User;
import com.bambi.service.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 카드 댓글 도메인 (Week3 SNS). 공개(PUBLIC) 카드에만 작성/조회, 작성자만 삭제.
 * 좋아요/스크랩과 같은 "공개 카드 대상" 정책 — 비공개/없음은 존재 노출 없이 404.
 */
@Service
public class CommentService {

    private static final String PUBLIC = "PUBLIC";

    private final CommentRepository commentRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository,
                          CardRepository cardRepository,
                          UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CommentResponse create(Long userId, String cardPublicId, CommentRequest req) {
        Card card = resolvePublicCard(cardPublicId);
        Comment comment = new Comment(card.getId(), userId, req.content().strip());
        commentRepository.save(comment);
        User author = userRepository.findById(userId).orElse(null);
        return CommentResponse.from(comment, author);
    }

    /** 카드 댓글 목록(오래된 순). 작성자는 1 IN 쿼리로 배치 로딩(N+1 차단). */
    @Transactional(readOnly = true)
    public List<CommentResponse> list(String cardPublicId) {
        Card card = resolvePublicCard(cardPublicId);
        List<Comment> comments = commentRepository.findByCardIdAndDeletedAtIsNullOrderByCreatedAtAsc(card.getId());
        if (comments.isEmpty()) {
            return List.of();
        }
        List<Long> authorIds = comments.stream().map(Comment::getUserId).distinct().toList();
        Map<Long, User> authors = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return comments.stream()
                .map(c -> CommentResponse.from(c, authors.get(c.getUserId())))
                .toList();
    }

    /** 댓글 삭제 — 작성자만(soft delete). 카드 공개 여부와 무관(비공개 전환돼도 내 댓글은 지울 수 있다). */
    @Transactional
    public void delete(Long userId, Long commentId) {
        Comment comment = commentRepository.findByIdAndUserIdAndDeletedAtIsNull(commentId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "댓글을 찾을 수 없습니다."));
        comment.softDelete();
    }

    /** 공개(PUBLIC) 카드만 댓글 대상. 형식 오류/없음/비공개는 모두 404 로 통일한다. */
    private Card resolvePublicCard(String cardPublicId) {
        UUID uuid;
        try {
            uuid = UUID.fromString(cardPublicId);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다.");
        }
        Card card = cardRepository.findByPublicIdAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다."));
        if (!PUBLIC.equals(card.getVisibility())) {
            throw new ApiException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다.");
        }
        return card;
    }
}
