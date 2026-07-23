package com.bambi.service.interest;

import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.interest.dto.InterestRequest;
import com.bambi.service.interest.dto.InterestResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관심사 CRUD (P0) — 소유자 범위 + soft delete + 이름 중복 방지.
 * note 템플릿과 같은 구조(Controller→Service→Repository, 권한은 userId 로 강제).
 * 이 API 로 만드는 관심사는 항상 source=USER(직접 입력). INFERRED 는 agent 몫(P1).
 */
@Service
public class InterestService {

    private final InterestRepository interestRepository;

    public InterestService(InterestRepository interestRepository) {
        this.interestRepository = interestRepository;
    }

    @Transactional
    public InterestResponse create(Long userId, InterestRequest req) {
        String name = req.name().strip();
        if (interestRepository.existsByUserIdAndNameAndDeletedAtIsNull(userId, name)) {
            throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "이미 등록한 관심사입니다.");
        }
        Interest interest = new Interest(userId, name);
        interestRepository.save(interest);
        return InterestResponse.from(interest);
    }

    @Transactional(readOnly = true)
    public List<InterestResponse> list(Long userId) {
        return interestRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId).stream()
                .map(InterestResponse::from)
                .toList();
    }

    @Transactional
    public InterestResponse rename(Long userId, Long interestId, InterestRequest req) {
        String name = req.name().strip();
        Interest interest = findOwned(userId, interestId);
        // 이름이 실제로 바뀔 때만 중복 검사 (자기 자신과의 충돌 제외)
        if (!interest.getName().equals(name)
                && interestRepository.existsByUserIdAndNameAndDeletedAtIsNull(userId, name)) {
            throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "이미 등록한 관심사입니다.");
        }
        interest.rename(name);   // dirty checking 으로 flush
        return InterestResponse.from(interest);
    }

    @Transactional
    public void delete(Long userId, Long interestId) {
        Interest interest = findOwned(userId, interestId);
        interest.softDelete();
    }

    /** 내 것(soft delete 제외)만 조회. 없으면 NOT_FOUND — 남의 것도 존재 노출 없이 404. */
    private Interest findOwned(Long userId, Long interestId) {
        return interestRepository.findByIdAndUserIdAndDeletedAtIsNull(interestId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "관심사를 찾을 수 없습니다."));
    }
}
