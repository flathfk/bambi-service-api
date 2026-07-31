package com.bambi.service.auth;

import com.bambi.service.auth.dto.AuthResponse;
import com.bambi.service.auth.dto.LoginRequest;
import com.bambi.service.auth.dto.SignupRequest;
import com.bambi.service.auth.dto.UserSummary;
import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.user.Role;
import com.bambi.service.user.RoleRepository;
import com.bambi.service.user.User;
import com.bambi.service.user.UserRegisteredEvent;
import com.bambi.service.user.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final ApplicationEventPublisher eventPublisher;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public UserSummary signup(SignupRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "이미 가입된 이메일입니다.");
        }
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR, "USER 권한 seed 가 없습니다."));

        User user = new User(req.email(), passwordEncoder.encode(req.password()), req.displayName());
        user.addRole(userRole);
        userRepository.save(user);

        // 가입 커밋 후 agent 에 컨텍스트를 1회 동기화한다(리스너가 AFTER_COMMIT 처리, 실패해도 가입 유지).
        eventPublisher.publishEvent(new UserRegisteredEvent(user.getId()));
        return UserSummary.from(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        List<String> roleNames = user.getRoles().stream().map(Role::getName).toList();
        String token = tokenProvider.createToken(user.getId(), user.getEmail(), roleNames);
        return AuthResponse.of(token, tokenProvider.getExpirationMinutes(), UserSummary.from(user));
    }

    /**
     * 현재 사용자 조회(me). 로그인과 동일한 UserSummary 로 통일 — 토큰만으로 만들던
     * {id,email} 응답이 프론트 User 타입(displayName·roles 필수)과 어긋나던 버그 픽스.
     * 토큰은 유효한데 사용자가 삭제된 경우는 인증 실패로 취급한다.
     */
    @Transactional(readOnly = true)
    public UserSummary me(Long userId) {
        User user = userRepository.findById(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_INVALID_TOKEN));
        return UserSummary.from(user);
    }
}
