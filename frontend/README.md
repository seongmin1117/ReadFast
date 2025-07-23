# ReadFast Admin Dashboard

ReadFast 시스템의 관리자 대시보드입니다. React + TypeScript로 구축되었으며, 인증 로그 관리 및 데이터 아카이빙 기능을 제공합니다.

## 🚀 기능

### 📊 대시보드
- 시스템 전체 현황 모니터링
- 주요 지표 및 통계 확인
- 최근 활동 로그 조회
- 시스템 알림 및 상태 확인

### 🔍 인증 로그 관리
- 실시간 로그 검색 및 필터링
- 고급 검색 (날짜 범위, 사용자, 디바이스, 결과 등)
- 페이지네이션 (오프셋 및 커서 기반)
- 로그 데이터 내보내기 (CSV, Excel, JSON)
- 상세 로그 정보 조회

### 💾 데이터 아카이빙
- 자동/수동 아카이빙 실행
- 아카이빙 상태 모니터링
- 스토리지 사용량 관리
- 아카이브 파일 다운로드
- 아카이빙 통계 및 성능 분석

### ⚙️ 설정 관리
- 데이터 보존 정책 설정
- 아카이빙 스케줄 관리
- 시스템 설정 구성

## 🛠 기술 스택

- **Frontend Framework**: React 18 + TypeScript
- **Build Tool**: Vite
- **UI Components**: Tailwind CSS + Custom Components
- **State Management**: React Query (TanStack Query)
- **Routing**: React Router v6
- **HTTP Client**: Axios
- **Form Handling**: React Hook Form + Zod
- **Date Handling**: date-fns
- **Icons**: Lucide React

## 📁 프로젝트 구조

```
frontend/
├── public/                     # 정적 파일
├── src/
│   ├── components/             # 재사용 가능한 컴포넌트
│   │   ├── ui/                # 기본 UI 컴포넌트
│   │   └── layout/            # 레이아웃 컴포넌트
│   ├── pages/                 # 페이지 컴포넌트
│   │   ├── Dashboard/         # 대시보드
│   │   ├── AuthLogs/          # 인증 로그
│   │   ├── Archiving/         # 아카이빙
│   │   └── Settings/          # 설정
│   ├── hooks/                 # 커스텀 React 훅
│   ├── services/              # API 클라이언트
│   ├── types/                 # TypeScript 타입 정의
│   ├── utils/                 # 유틸리티 함수
│   ├── styles/                # 전역 스타일
│   ├── App.tsx               # 메인 앱 컴포넌트
│   └── main.tsx              # 엔트리 포인트
├── package.json
├── tsconfig.json
├── vite.config.ts
└── tailwind.config.js
```

## 🚀 시작하기

### 전제 조건
- Node.js 18+ 
- npm 또는 yarn

### 설치 및 실행

1. **의존성 설치**
   ```bash
   cd frontend
   npm install
   ```

2. **환경 변수 설정**
   ```bash
   cp .env.example .env.local
   ```
   
   `.env.local` 파일에서 백엔드 API URL을 설정:
   ```
   VITE_API_BASE_URL=http://localhost:8080/api
   ```

3. **개발 서버 실행**
   ```bash
   npm run dev
   ```
   
   브라우저에서 `http://localhost:5173` 접속

4. **빌드**
   ```bash
   npm run build
   ```

5. **프리뷰**
   ```bash
   npm run preview
   ```

## 🔧 개발 명령어

```bash
# 개발 서버 실행
npm run dev

# 프로덕션 빌드
npm run build

# 빌드 결과 프리뷰
npm run preview

# 린트 검사
npm run lint

# 린트 자동 수정
npm run lint:fix

# 타입 체크
npm run type-check
```

## 🌐 API 연동

백엔드 서버와의 연동을 위해 다음 API 엔드포인트를 사용합니다:

### 인증 로그 API
- `GET /api/v1/auth/search` - V1 API (오프셋 기반)
- `GET /api/v2/auth/search` - V2 API (커서 기반)  
- `GET /api/v3/auth/search` - V3 API (통합 조회)

### 아카이빙 API
- `GET /api/v3/auth/archiving/execute` - 수동 아카이빙 실행
- `GET /api/v3/auth/archiving/status` - 아카이빙 상태 조회

### 정책 관리 API
- `GET /api/v1/policies/{id}` - 정책 조회
- `PUT /api/v1/policies/{id}` - 정책 업데이트

## 🎨 UI/UX 특징

### 반응형 디자인
- 모바일, 태블릿, 데스크톱 최적화
- Tailwind CSS를 활용한 유틸리티 우선 스타일링

### 접근성
- WCAG 2.1 AA 준수
- 키보드 네비게이션 지원
- 스크린 리더 호환성

### 성능 최적화
- React Query를 통한 서버 상태 관리
- 가상화를 통한 대용량 데이터 처리
- 코드 스플리팅 및 레이지 로딩
- 이미지 최적화

## 🧪 테스트

```bash
# 단위 테스트 실행 (향후 추가 예정)
npm run test

# 테스트 커버리지
npm run test:coverage

# E2E 테스트 (향후 추가 예정)
npm run test:e2e
```

## 📦 배포

### 개발 환경
```bash
npm run build
npm run preview
```

### 프로덕션 환경
```bash
# 환경 변수 설정
export VITE_API_BASE_URL=https://api.readfast.com
export VITE_NODE_ENV=production

# 빌드
npm run build

# 빌드된 파일을 웹 서버에 배포
cp -r dist/* /var/www/html/
```

## 🔒 보안 고려사항

- API 키 및 민감한 정보는 환경 변수로 관리
- XSS 공격 방지를 위한 입력값 검증
- CORS 설정 확인
- HTTPS 사용 권장

## 🤝 기여 가이드

1. **코드 스타일**: ESLint + Prettier 설정을 따릅니다
2. **커밋 메시지**: Conventional Commits 규약을 준수합니다
3. **브랜치**: `feature/기능명` 형태로 생성합니다
4. **테스트**: 새로운 기능 추가 시 테스트 코드를 포함합니다

## 📞 지원

문제가 발생하거나 질문이 있으시면:
1. GitHub Issues에서 이슈를 등록해주세요
2. 개발팀에 문의해주세요

## 📄 라이선스

이 프로젝트는 [MIT 라이선스](LICENSE)를 따릅니다.