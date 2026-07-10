package com.bambi.service.note;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    /** 소유자 범위 + soft delete 제외 조회 (권한/삭제 규칙을 쿼리에서 강제) */
    List<Note> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    Optional<Note> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);
}
