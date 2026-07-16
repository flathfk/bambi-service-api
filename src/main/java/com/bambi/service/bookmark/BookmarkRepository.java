package com.bambi.service.bookmark;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    /** 소유자 범위 + soft delete 제외 목록 (최신순) */
    List<Bookmark> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    Optional<Bookmark> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    /** 같은 유저의 살아있는 동일 URL 중복 방지 (DB 유니크 인덱스 uq_bookmarks_user_url 의 사전 검사) */
    boolean existsByUserIdAndUrlAndDeletedAtIsNull(Long userId, String url);
}
