package com.bambi.service.comment;

import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.comment.dto.CommentRequest;
import com.bambi.service.comment.dto.CommentResponse;
import com.bambi.service.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 카드 댓글 (Week3 SNS). 인증 필수. 대상 카드는 publicId(UUID)로 가리킨다.
 * 공개(PUBLIC) 카드에만 작성/조회, 삭제는 작성자만.
 */
@RestController
@RequestMapping("/api/cards/{publicId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> create(@AuthenticationPrincipal AuthPrincipal principal,
                                               @PathVariable String publicId,
                                               @Valid @RequestBody CommentRequest request) {
        return ApiResponse.ok(commentService.create(principal.id(), publicId, request));
    }

    @GetMapping
    public ApiResponse<List<CommentResponse>> list(@PathVariable String publicId) {
        return ApiResponse.ok(commentService.list(publicId));
    }

    @DeleteMapping("/{commentId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthPrincipal principal,
                                    @PathVariable String publicId,
                                    @PathVariable Long commentId) {
        commentService.delete(principal.id(), commentId);
        return ApiResponse.ok();
    }
}
