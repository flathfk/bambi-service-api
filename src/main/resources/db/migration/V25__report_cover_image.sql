-- Agent가 실제 인용 출처에서 선택한 대표 이미지와 원문 출처를 리포트에 보존한다.
-- 이미지가 없거나 수집에 실패한 기존·신규 리포트는 모두 null로 유지한다.

ALTER TABLE service.reports
    ADD COLUMN cover_image_url TEXT,
    ADD COLUMN cover_image_source_url TEXT,
    ADD COLUMN cover_image_source_title VARCHAR(500),
    ADD COLUMN cover_image_reference VARCHAR(20);

COMMENT ON COLUMN service.reports.cover_image_url IS
    '리포트 상단에 표시할 실제 인용 출처의 대표 이미지 URL';
COMMENT ON COLUMN service.reports.cover_image_source_url IS
    '대표 이미지가 연결된 실제 인용 원문 URL';
COMMENT ON COLUMN service.reports.cover_image_source_title IS
    '화면 이미지 출처 표시에 사용할 원문 제목';
COMMENT ON COLUMN service.reports.cover_image_reference IS
    'Agent가 대표 이미지 선택에 사용한 citation 참조(P/G/L)';
