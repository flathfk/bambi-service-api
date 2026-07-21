package com.bambi.service.admin;

import com.bambi.service.admin.dto.AdminUserResponse;
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
}
