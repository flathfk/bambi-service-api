package com.bambi.service.note;

import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.common.response.ApiResponse;
import com.bambi.service.note.dto.NoteRequest;
import com.bambi.service.note.dto.NoteResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * reference CRUD 템플릿 컨트롤러. 모든 요청은 인증 필수(SecurityConfig).
 * 현재 사용자 id 는 @AuthenticationPrincipal 로 꺼내 소유자 범위를 강제한다.
 */
@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NoteResponse> create(@AuthenticationPrincipal AuthPrincipal principal,
                                            @Valid @RequestBody NoteRequest request) {
        return ApiResponse.ok(noteService.create(principal.id(), request));
    }

    @GetMapping
    public ApiResponse<List<NoteResponse>> list(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(noteService.list(principal.id()));
    }

    @GetMapping("/{id}")
    public ApiResponse<NoteResponse> get(@AuthenticationPrincipal AuthPrincipal principal,
                                         @PathVariable Long id) {
        return ApiResponse.ok(noteService.get(principal.id(), id));
    }

    @PutMapping("/{id}")
    public ApiResponse<NoteResponse> update(@AuthenticationPrincipal AuthPrincipal principal,
                                            @PathVariable Long id,
                                            @Valid @RequestBody NoteRequest request) {
        return ApiResponse.ok(noteService.update(principal.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthPrincipal principal,
                                    @PathVariable Long id) {
        noteService.delete(principal.id(), id);
        return ApiResponse.ok();
    }
}
