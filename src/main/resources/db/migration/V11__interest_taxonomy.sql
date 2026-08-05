-- 관심사 분류체계를 Service DB의 버전 관리 원본으로 승격한다.
-- 프론트는 Service API로 이 카탈로그를 조회하고, Agent에는 같은 버전 Snapshot을 전달한다.

CREATE TABLE service.interest_taxonomy_versions (
    version         VARCHAR(50) PRIMARY KEY,
    status          VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'RETIRED')),
    source_hash     VARCHAR(64) NOT NULL CHECK (length(source_hash) = 64),
    locale          VARCHAR(16) NOT NULL DEFAULT 'ko-KR',
    published_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_interest_taxonomy_active
    ON service.interest_taxonomy_versions ((status))
    WHERE status = 'ACTIVE';

CREATE TABLE service.interest_categories (
    id                  BIGSERIAL PRIMARY KEY,
    taxonomy_version    VARCHAR(50) NOT NULL
        REFERENCES service.interest_taxonomy_versions(version),
    category_key        VARCHAR(50) NOT NULL,
    name                VARCHAR(100) NOT NULL,
    name_en             VARCHAR(100),
    description         VARCHAR(300),
    emoji               VARCHAR(16),
    display_order       INT NOT NULL CHECK (display_order > 0),
    UNIQUE (taxonomy_version, category_key),
    UNIQUE (taxonomy_version, display_order)
);

CREATE TABLE service.interest_topics (
    id                  BIGSERIAL PRIMARY KEY,
    taxonomy_version    VARCHAR(50) NOT NULL,
    topic_key           VARCHAR(50) NOT NULL,
    category_key        VARCHAR(50) NOT NULL,
    name                VARCHAR(100) NOT NULL,
    name_en             VARCHAR(100),
    description         VARCHAR(300),
    display_order       INT NOT NULL CHECK (display_order > 0),
    keywords            JSONB NOT NULL DEFAULT '[]'::jsonb,
    FOREIGN KEY (taxonomy_version, category_key)
        REFERENCES service.interest_categories(taxonomy_version, category_key),
    UNIQUE (taxonomy_version, topic_key),
    UNIQUE (taxonomy_version, category_key, display_order),
    CHECK (jsonb_typeof(keywords) = 'array')
);

ALTER TABLE service.interests
    ADD COLUMN taxonomy_version VARCHAR(50),
    ADD COLUMN taxonomy_category_id VARCHAR(50),
    ADD COLUMN taxonomy_topic_id VARCHAR(50),
    ADD CONSTRAINT ck_interests_taxonomy_selection CHECK (
        (taxonomy_version IS NULL
            AND taxonomy_category_id IS NULL
            AND taxonomy_topic_id IS NULL)
        OR
        (taxonomy_version IS NOT NULL
            AND taxonomy_category_id IS NOT NULL
            AND taxonomy_topic_id IS NOT NULL)
    ),
    ADD CONSTRAINT fk_interests_taxonomy_category FOREIGN KEY (
        taxonomy_version,
        taxonomy_category_id
    ) REFERENCES service.interest_categories(taxonomy_version, category_key),
    ADD CONSTRAINT fk_interests_taxonomy_topic FOREIGN KEY (
        taxonomy_version,
        taxonomy_topic_id
    ) REFERENCES service.interest_topics(taxonomy_version, topic_key);

CREATE INDEX idx_interests_taxonomy_topic
    ON service.interests(taxonomy_version, taxonomy_topic_id)
    WHERE deleted_at IS NULL AND taxonomy_topic_id IS NOT NULL;

INSERT INTO service.interest_taxonomy_versions (
    version, status, source_hash, locale
) VALUES (
    '1.0.0-draft', 'ACTIVE',
    '0e6a22eb7e22986ad5b00f7f4c25190db6289d6ff3b14c45c9e67da643d1175f',
    'ko-KR'
);

INSERT INTO service.interest_categories (
    taxonomy_version, category_key, name, name_en, description, emoji, display_order
) VALUES
('1.0.0-draft', 'tech', '테크·IT', 'Tech & IT', '새 기술과 서비스, 개발 현장 소식', '💻', 1),
('1.0.0-draft', 'business', '비즈니스·경제', 'Business & Economy', '시장과 기업, 돈의 흐름', '📊', 2),
('1.0.0-draft', 'career', '커리어·자기계발', 'Career & Growth', '일하는 방식과 성장', '🚀', 3),
('1.0.0-draft', 'science', '과학·환경', 'Science & Environment', '연구 성과와 지구·우주 이야기', '🔬', 4),
('1.0.0-draft', 'health', '건강·웰빙', 'Health & Wellbeing', '몸과 마음을 관리하는 법', '🌿', 5),
('1.0.0-draft', 'society', '시사·사회', 'News & Society', '지금 벌어지는 일들', '📰', 6),
('1.0.0-draft', 'culture', '문화·엔터', 'Culture & Entertainment', '보고 듣고 읽는 것들', '🎬', 7),
('1.0.0-draft', 'life', '라이프·취미', 'Life & Hobbies', '일상을 채우는 관심사', '🌈', 8);

INSERT INTO service.interest_topics (
    taxonomy_version, topic_key, category_key, name, name_en, description,
    display_order, keywords
) VALUES
('1.0.0-draft', 'ai_ml', 'tech', 'AI·머신러닝', 'AI & Machine Learning', '생성형 AI, LLM, 모델 연구와 활용 사례', 1, '["생성형 AI","LLM","머신러닝","AI 에이전트","OpenAI","Anthropic"]'),
('1.0.0-draft', 'programming', 'tech', '개발·프로그래밍', 'Software Development', '언어·프레임워크·오픈소스, 개발자 이야기', 2, '["오픈소스","프레임워크","리팩터링","개발자","GitHub","programming"]'),
('1.0.0-draft', 'data_cloud', 'tech', '데이터·클라우드', 'Data & Cloud', '데이터 엔지니어링, 인프라, 클라우드 서비스', 3, '["데이터 파이프라인","클라우드","쿠버네티스","AWS","데이터베이스","DevOps"]'),
('1.0.0-draft', 'security', 'tech', '보안·프라이버시', 'Security & Privacy', '해킹 사고, 취약점, 개인정보 보호', 4, '["해킹","취약점","개인정보 유출","랜섬웨어","보안 패치","cybersecurity"]'),
('1.0.0-draft', 'gadget', 'tech', '가젯·디바이스', 'Gadgets & Devices', '스마트폰, 노트북, 웨어러블 신제품과 리뷰', 5, '["스마트폰","노트북","웨어러블","신제품 리뷰","애플","갤럭시"]'),
('1.0.0-draft', 'mobility', 'tech', '자동차·모빌리티', 'Cars & Mobility', '전기차, 자율주행, 이동 수단의 변화', 6, '["전기차","자율주행","배터리","모빌리티","테슬라","EV"]'),
('1.0.0-draft', 'startup', 'business', '스타트업·창업', 'Startups', '창업 사례, 투자 유치, 팀 빌딩', 1, '["스타트업","시리즈 A","투자 유치","창업","VC","액셀러레이터"]'),
('1.0.0-draft', 'invest', 'business', '투자·재테크', 'Investing', '주식·코인·연금 등 개인 자산 운용', 2, '["주식","ETF","가상자산","연금","배당","포트폴리오"]'),
('1.0.0-draft', 'economy', 'business', '경제·금융', 'Economy & Finance', '금리, 환율, 물가 같은 거시 경제 흐름', 3, '["금리","환율","물가","경기 전망","중앙은행","인플레이션"]'),
('1.0.0-draft', 'industry', 'business', '산업·기업', 'Industry & Companies', '빅테크와 주요 산업의 전략과 실적', 4, '["실적 발표","빅테크","반도체","인수합병","산업 동향","공급망"]'),
('1.0.0-draft', 'marketing', 'business', '마케팅·브랜드', 'Marketing & Brand', '캠페인, 브랜딩, 소비자 트렌드', 5, '["마케팅","브랜딩","광고 캠페인","소비 트렌드","그로스","리테일"]'),
('1.0.0-draft', 'realestate', 'business', '부동산', 'Real Estate', '집값, 청약, 임대차 정책', 6, '["부동산","집값","청약","전세","재건축","주택 정책"]'),
('1.0.0-draft', 'job', 'career', '커리어·이직', 'Career & Job Change', '채용 시장, 이직 준비, 연봉 협상', 1, '["이직","채용","면접","연봉 협상","커리어","포트폴리오"]'),
('1.0.0-draft', 'productivity', 'career', '생산성·업무도구', 'Productivity & Tools', '일하는 방법, 노션·AI 도구 활용', 2, '["생산성","업무 자동화","노션","AI 도구","워크플로","시간 관리"]'),
('1.0.0-draft', 'study', 'career', '공부·학습법', 'Learning', '외국어, 자격증, 효율적인 공부 방법', 3, '["공부법","영어 학습","자격증","온라인 강의","복습","학습 습관"]'),
('1.0.0-draft', 'writing', 'career', '글쓰기·기록', 'Writing & Note-taking', '글쓰기, 메모, 지식 정리', 4, '["글쓰기","메모","지식 관리","제텔카스텐","뉴스레터","옵시디언"]'),
('1.0.0-draft', 'leadership', 'career', '리더십·조직문화', 'Leadership & Culture', '매니징, 협업, 일하는 문화', 5, '["리더십","조직문화","매니징","원온원","협업","재택근무"]'),
('1.0.0-draft', 'space', 'science', '우주·천문', 'Space & Astronomy', '우주 탐사, 발사체, 천문 발견', 1, '["우주","천문","위성","화성","NASA","스페이스X"]'),
('1.0.0-draft', 'bio', 'science', '생명과학·의학', 'Life Science & Medicine', '유전자, 신약, 의학 연구', 2, '["유전자","신약","임상시험","바이오","면역","생명공학"]'),
('1.0.0-draft', 'climate', 'science', '기후·환경', 'Climate & Environment', '기후 변화, 에너지 전환, 생태계', 3, '["기후 변화","탄소중립","재생에너지","생태계","미세먼지","ESG"]'),
('1.0.0-draft', 'brain', 'science', '뇌과학·심리', 'Neuroscience & Psychology', '인지, 감정, 행동에 대한 연구', 4, '["뇌과학","심리학","인지","행동경제학","기억","의사결정"]'),
('1.0.0-draft', 'physics_math', 'science', '물리·수학', 'Physics & Math', '기초 과학 이론과 새로운 발견', 5, '["물리학","수학","양자","입자물리","수학 난제","기초과학"]'),
('1.0.0-draft', 'fitness', 'health', '운동·피트니스', 'Fitness', '근력, 러닝, 홈트레이닝', 1, '["운동","근력 운동","러닝","홈트레이닝","체지방","스트레칭"]'),
('1.0.0-draft', 'nutrition', 'health', '식단·영양', 'Nutrition & Diet', '먹는 것과 몸의 관계, 다이어트', 2, '["식단","영양","다이어트","단백질","혈당","영양제"]'),
('1.0.0-draft', 'mental', 'health', '멘탈·스트레스', 'Mental Health', '번아웃, 불안, 마음 돌보기', 3, '["번아웃","불안","명상","스트레스 관리","정신건강","마음챙김"]'),
('1.0.0-draft', 'sleep', 'health', '수면·회복', 'Sleep & Recovery', '잘 자는 법, 피로 회복 루틴', 4, '["수면","불면","피로 회복","생활 리듬","낮잠","수면의 질"]'),
('1.0.0-draft', 'medical', 'health', '질환·의료정보', 'Medical Info', '질병 정보, 검진, 의료 제도', 5, '["건강검진","질환","예방접종","의료 정책","약","병원"]'),
('1.0.0-draft', 'korea', 'society', '국내 이슈', 'Korea News', '지금 한국에서 벌어지는 일', 1, '["국내 뉴스","정치","사건 사고","지역","국회","행정"]'),
('1.0.0-draft', 'world', 'society', '국제 정세', 'World Affairs', '해외 뉴스, 외교와 분쟁', 2, '["국제 정세","외교","분쟁","미중 관계","무역","geopolitics"]'),
('1.0.0-draft', 'policy', 'society', '정책·제도', 'Policy & Regulation', '새로 바뀌는 법과 제도', 3, '["정책","규제","법 개정","제도 개편","세금","AI 규제"]'),
('1.0.0-draft', 'social', 'society', '사회·노동', 'Society & Labor', '일자리, 인구, 불평등 같은 사회 문제', 4, '["노동","일자리","인구","저출생","불평등","복지"]'),
('1.0.0-draft', 'media', 'society', '미디어·언론', 'Media & Journalism', '뉴스가 만들어지는 방식, 플랫폼 변화', 5, '["언론","저널리즘","가짜뉴스","플랫폼","여론","팩트체크"]'),
('1.0.0-draft', 'movie_drama', 'culture', '영화·드라마', 'Film & TV', '개봉작, 시리즈, OTT 화제작', 1, '["영화","드라마","넷플릭스","OTT","개봉","시리즈"]'),
('1.0.0-draft', 'music', 'culture', '음악', 'Music', '신보, 공연, 아티스트 소식', 2, '["음악","신보","공연","K-POP","앨범","페스티벌"]'),
('1.0.0-draft', 'book', 'culture', '책·문학', 'Books & Literature', '신간, 서평, 읽을거리', 3, '["신간","서평","베스트셀러","문학","출판","독서"]'),
('1.0.0-draft', 'game', 'culture', '게임', 'Games', '신작, 업데이트, 게임 산업', 4, '["게임","신작","e스포츠","스팀","콘솔","인디게임"]'),
('1.0.0-draft', 'webtoon_anime', 'culture', '웹툰·애니', 'Webtoon & Anime', '연재작, 애니메이션, 원작 IP', 5, '["웹툰","애니메이션","만화","IP","연재","webtoon"]'),
('1.0.0-draft', 'art_design', 'culture', '예술·디자인', 'Art & Design', '전시, 건축, 디자인 트렌드', 6, '["전시","디자인","건축","미술","타이포그래피","UX 디자인"]'),
('1.0.0-draft', 'travel', 'life', '여행', 'Travel', '여행지, 항공·숙소, 여행 팁', 1, '["여행","항공권","숙소","여행지","해외여행","국내여행"]'),
('1.0.0-draft', 'food', 'life', '음식·요리', 'Food & Cooking', '레시피, 맛집, 식재료', 2, '["요리","레시피","맛집","식재료","베이킹","커피"]'),
('1.0.0-draft', 'sports', 'life', '스포츠', 'Sports', '경기 결과, 리그, 선수 소식', 3, '["스포츠","축구","야구","리그","선수","올림픽"]'),
('1.0.0-draft', 'fashion_beauty', 'life', '패션·뷰티', 'Fashion & Beauty', '스타일, 화장품, 시즌 트렌드', 4, '["패션","뷰티","화장품","스타일링","트렌드","스킨케어"]'),
('1.0.0-draft', 'home', 'life', '홈·인테리어', 'Home & Interior', '집 꾸미기, 살림, 정리', 5, '["인테리어","살림","정리수납","가구","홈데코","자취"]'),
('1.0.0-draft', 'pet', 'life', '반려동물', 'Pets', '반려동물 건강, 훈련, 용품', 6, '["반려동물","강아지","고양이","펫","훈련","동물병원"]');
