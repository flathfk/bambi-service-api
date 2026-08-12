package com.bambi.service.briefing;

/** 정기 생성 시점에 Agent의 날짜별 아침 브리핑 Snapshot이 아직 준비되지 않은 상태. */
public class BriefingPreparationPendingException extends RuntimeException {

    public BriefingPreparationPendingException(long userId) {
        super("아침 브리핑 Wiki 준비가 완료되지 않았습니다. userId=" + userId);
    }
}
