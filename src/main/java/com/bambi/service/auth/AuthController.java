package com.bambi.service.auth;

import com.bambi.service.auth.dto.AuthResponse;
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

    /** 현재 로그인 사용자 확인 (토큰 검증용) */
    @GetMapping("/me")
    public ApiResponse<AuthPrincipal> me(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(principal);
    }
}
