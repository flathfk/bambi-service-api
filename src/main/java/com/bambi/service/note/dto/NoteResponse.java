package com.bambi.service.note.dto;

import com.bambi.service.note.Note;

import java.time.OffsetDateTime;

public record NoteResponse(
        Long id,
        String title,
        String content,
        OffsetDateTime createdAt) {

    public static NoteResponse from(Note note) {
        return new NoteResponse(note.getId(), note.getTitle(), note.getContent(), note.getCreatedAt());
    }
}
