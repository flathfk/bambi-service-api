package com.bambi.service.bookmark;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    /** 소유자 범위 + soft delete 제외 목록 (최신순) */
    List<Bookmark> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    Optional<Bookmark> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    /** 같은 유저의 살아있는 동일 URL 북마크를 재클리핑 upsert 대상으로 조회한다. */
    Optional<Bookmark> findByUserIdAndUrlAndDeletedAtIsNull(Long userId, String url);
}
