package com.bambi.service.interest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterestRepository extends JpaRepository<Interest, Long> {

    /** 소유자 범위 + soft delete 제외 목록 (최신순) */
    List<Interest> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    /** Agent 컨텍스트에 보낼 사용자 직접 설정 관심사를 이름순으로 조회한다. */
    List<Interest> findByUserIdAndSourceAndDeletedAtIsNullOrderByNameAsc(
            Long userId, InterestSource source);

    Optional<Interest> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    /** 같은 유저의 살아있는 동일 이름 중복 방지 (DB 유니크 인덱스 uq_interests_user_name 의 사전 검사) */
    boolean existsByUserIdAndNameAndDeletedAtIsNull(Long userId, String name);
}
