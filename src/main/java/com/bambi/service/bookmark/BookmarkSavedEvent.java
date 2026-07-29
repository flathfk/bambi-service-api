package com.bambi.service.bookmark;

/**
 * 관심 자료(북마크) 저장 완료 이벤트.
 * 저장 트랜잭션 커밋 후 agent 위키 원천 처리로 중계하기 위해 발행한다(가입 컨텍스트 동기화와 같은 패턴).
 * 중계 분기(clippings vs urls)에 필요한 값을 담아, 리스너가 북마크를 재조회하지 않게 한다.
 *
 * @param userId     저장한 사용자 ID
 * @param bookmarkId 저장된 북마크 ID (멱등 키 {@code bookmark-{id}} 로 쓰인다)
 * @param url        저장 URL (없으면 null)
 * @param title      제목 (없으면 null)
 * @param content    본문/메모 (없으면 null)
 */
public record BookmarkSavedEvent(long userId, long bookmarkId, String url, String title, String content) {
}
