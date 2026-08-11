package com.bambi.service.report;

import com.bambi.service.report.dto.ReportResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 리포트 상세 응답의 대표 이미지 계약을 검증한다. */
class ReportResponseTest {

    @Test
    void 대표_이미지와_원문_출처를_상세_응답에_포함한다() {
        Report report = Report.fromExternal(1L, "content-1", 1, "제목", "요약", "본문");
        report.applyCoverImage(
                "https://cdn.example.com/cover.jpg",
                "https://news.example.com/article",
                "기사 제목",
                "G1");

        ReportResponse response = ReportResponse.from(report);

        assertThat(response.coverImage()).isNotNull();
        assertThat(response.coverImage().url()).isEqualTo("https://cdn.example.com/cover.jpg");
        assertThat(response.coverImage().sourceUrl()).isEqualTo("https://news.example.com/article");
        assertThat(response.coverImage().sourceTitle()).isEqualTo("기사 제목");
        assertThat(response.coverImage().reference()).isEqualTo("G1");
    }
}
