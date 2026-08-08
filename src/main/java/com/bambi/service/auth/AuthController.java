package com.bambi.service.auth;

import com.bambi.service.auth.dto.AuthResponse;
import com.bambi.service.auth.dto.ChangePasswordRequest;
import com.bambi.service.auth.dto.LoginRequest;
import com.bambi.service.auth.dto.SignupRequest;
import com.bambi.service.auth.dto.UserSummary;
import com.bambi.service.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 회원가입 (USER 권한 부여) */
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserSummary> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.ok(authService.signup(request));
    }

    /** 로그인 → access token 발급 */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    /** 현재 로그인 사용자 확인. 로그인 응답과 동일한 UserSummary 를 준다(프론트 User 타입 정합). */
    @GetMapping("/me")
    public ApiResponse<UserSummary> me(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(authService.me(principal.id()));
    }

    /**
     * 비밀번호 변경 (로그인 필요). 현재 비밀번호 확인 후 교체.
     * 현재 비밀번호 불일치 → 401, 새 비밀번호 정책 위반(@Valid) → 400.
     * ⚠️ stateless JWT 라 변경 후에도 기존 토큰은 만료까지 유효하다.
     */
    @PostMapping("/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal AuthPrincipal principal,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.id(), request);
        return ApiResponse.ok();
    }
}
