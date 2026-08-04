package com.bambi.service.admin;

import com.bambi.service.admin.dto.AdminUserResponse;
import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.user.User;
import com.bambi.service.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관리자용 사용자 조회.
 *
 * 도메인 서비스(Note 등)가 "내 것"만 보는 것과 달리, 여기선 전체 사용자를 훑는다.
 * 그만큼 접근 통제가 중요한데, 권한 검사는 SecurityConfig 의 /api/admin/** = ADMIN 한 곳에
 * 모아 두었으므로 서비스 계층은 조회 로직에만 집중한다.
 */
@Service
public class AdminUserService {

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 전체 사용자를 가입 최신순으로 반환한다.
     * 탈퇴(soft delete)한 계정도 빼지 않고 status 로 구분해 함께 보여준다 — 관리자는
     * "지금 활성인 사람"뿐 아니라 "있었던 사람"도 봐야 하기 때문.
     */
    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    /**
     * 사용자를 활성/비활성 전환한다(관리자 토글). 비활성은 soft delete 시각으로 표시하며,
     * 같은 상태로의 재요청은 그대로 둔다(멱등). 없는 사용자면 NOT_FOUND.
     */
    @Transactional
    public AdminUserResponse setActive(long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND,
                        "사용자를 찾을 수 없습니다 (id=" + userId + ")"));
        if (active) {
            user.activate();
        } else {
            user.deactivate();
        }
        return AdminUserResponse.from(user);
    }
}
