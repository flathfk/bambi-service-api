package com.bambi.service.auth;

/**
 * SecurityContext 에 담기는 인증 주체. 컨트롤러에서 @AuthenticationPrincipal 로 꺼내
 * 현재 사용자 id 를 얻는다. (JWT claim 으로 구성 — DB 조회 없음)
 */
public record AuthPrincipal(Long id, String email) {
}
