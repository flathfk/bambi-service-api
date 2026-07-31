package com.bambi.service.user;

import com.bambi.service.auth.dto.UserSummary;
import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.user.dto.UpdateProfileRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * 사용자 프로필 편집(07-31, 메뉴 전면 연결).
 * 핸들(username)은 소문자·숫자·언더스코어 3~30자 — 정규화(trim·소문자) 후 검증한다.
 * 유일성은 여기서 선검사하고 DB unique 제약이 최종 방어(경합 시 저장에서 터짐).
 */
@Service
public class UserService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9_]{3,30}$");

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserSummary updateProfile(Long userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_INVALID_TOKEN));

        user.updateProfile(req.displayName().strip(), normalizeBio(req.bio()));

        String username = normalizeUsername(req.username());
        if (username != null && !username.equals(user.getUsername())) {
            if (!USERNAME_PATTERN.matcher(username).matches()) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR,
                        "핸들은 영문 소문자·숫자·밑줄 3~30자여야 합니다.");
            }
            if (userRepository.existsByUsernameAndIdNot(username, userId)) {
                throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "이미 사용 중인 핸들입니다.");
            }
            user.changeUsername(username);
        }
        return UserSummary.from(user);
    }

    /** 빈 문자열 소개는 "지움"으로 취급해 null 저장. */
    private String normalizeBio(String bio) {
        if (bio == null) {
            return null;
        }
        String stripped = bio.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    /** 미전송/빈값 핸들은 "변경 없음". 대문자 입력은 소문자로 흡수한다. */
    private String normalizeUsername(String username) {
        if (username == null) {
            return null;
        }
        String normalized = username.strip().toLowerCase();
        return normalized.isEmpty() ? null : normalized;
    }
}
