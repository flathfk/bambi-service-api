package com.bambi.service.common.error;

/**
 * 도메인/서비스 계층에서 던지는 공통 예외.
 * GlobalExceptionHandler 가 ErrorCode 의 status/code 로 변환한다.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
