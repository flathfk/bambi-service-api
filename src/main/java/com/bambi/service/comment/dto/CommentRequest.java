package com.bambi.service.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 댓글 작성 요청. content 필수(1~1000자). */
public record CommentRequest(
        @NotBlank @Size(max = 1000) String content) {
}
