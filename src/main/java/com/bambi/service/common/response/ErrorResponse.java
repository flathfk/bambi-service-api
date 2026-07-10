package com.bambi.service.common.response;

/**
 * 실패 응답의 error 본문. HTTP status 는 별도(응답 상태코드)로 내려가고,
 * 여기 code 는 클라이언트가 분기하기 쉬운 내부 코드다.
 */
public record ErrorResponse(String code, String message) {
}
