package com.bambi.service.user;

import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.auth.dto.UserSummary;
import com.bambi.service.common.response.ApiResponse;
import com.bambi.service.user.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 본인 리소스. 프로필 편집(목업 profile-self.html 편집 모달)만 제공한다.
 * 조회는 공개 프로필(/api/users/{publicId}/profile)과 me(/api/auth/me)가 담당.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/me")
    public ApiResponse<UserSummary> updateMe(@AuthenticationPrincipal AuthPrincipal principal,
                                             @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateProfile(principal.id(), request));
    }
}
