package com.bambi.service.note;

import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.note.dto.NoteRequest;
import com.bambi.service.note.dto.NoteResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * reference CRUD 템플릿 — 소유자 범위 검증 + soft delete 패턴 시범.
 * 팀원은 이 구조(Controller→Service→Repository, 권한은 userId 로 강제)를 복붙해 도메인을 만든다.
 */
@Service
public class NoteService {

    private final NoteRepository noteRepository;

    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    @Transactional
    public NoteResponse create(Long userId, NoteRequest req) {
        Note note = new Note(userId, req.title(), req.content());
        noteRepository.save(note);
        return NoteResponse.from(note);
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> list(Long userId) {
        return noteRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId).stream()
                .map(NoteResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public NoteResponse get(Long userId, Long noteId) {
        return NoteResponse.from(findOwned(userId, noteId));
    }

    @Transactional
    public NoteResponse update(Long userId, Long noteId, NoteRequest req) {
        Note note = findOwned(userId, noteId);
        note.update(req.title(), req.content());
        return NoteResponse.from(note);   // dirty checking 으로 flush
    }

    @Transactional
    public void delete(Long userId, Long noteId) {
        Note note = findOwned(userId, noteId);
        note.softDelete();
    }

    /** 내 것(soft delete 제외)만 조회. 없으면 NOT_FOUND — 남의 것도 존재 노출 없이 404. */
    private Note findOwned(Long userId, Long noteId) {
        return noteRepository.findByIdAndUserIdAndDeletedAtIsNull(noteId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "노트를 찾을 수 없습니다."));
    }
}
