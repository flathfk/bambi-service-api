package com.bambi.service.agent;

/**
 * agent 가 컨텍스트 동기화를 409 STALE_CONTEXT_VERSION 으로 거절하면서
 * 현재 버전({@code current_context_version})을 함께 내려준 경우 던진다.
 * 호출부(AgentContextSyncService)가 이 버전에 맞춰 재전송하도록 하는 내부 신호다
 * (두 카운터 정합용 — 밖으로 새는 오류가 아니다).
 */
public class StaleContextVersionException extends RuntimeException {

    private final int currentVersion;

    public StaleContextVersionException(int currentVersion) {
        super("agent context STALE — current_context_version=" + currentVersion);
        this.currentVersion = currentVersion;
    }

    public int currentVersion() {
        return currentVersion;
    }
}
