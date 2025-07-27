# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 저장소에서 작업할 때 필요한 가이드를 제공합니다.

## 프로젝트 개요

ReadFast는 인증 로그 조회 및 데이터 보관 정책 관리를 위한 엔터프라이즈급 Spring Boot 애플리케이션입니다. 헥사고날 아키텍처를 기반으로 하며, 세 가지 다른 검색 구현(V1, V2, V3)과 고급 아카이빙 시스템, React + TypeScript 기반의 관리 대시보드를 포함합니다.

## 개발 명령어

### 빌드 및 실행
```bash
# 프로젝트 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun

# 테스트 실행
./gradlew test

# 클린 빌드
./gradlew clean build
```

### 데이터베이스 설정
```bash
# Docker를 통한 MySQL Master-Slave 및 PMM 모니터링 시작
docker-compose up -d

# 데이터베이스 중지
docker-compose down

# PMM 모니터링 접속 (admin/admin)
# http://localhost:8081
```

### 코드 품질
```bash
# 의존성 확인
./gradlew dependencies

# QueryDSL Q-클래스 생성 (필요한 경우)
./gradlew compileJava
```

### 프론트엔드 개발
```bash
# 프론트엔드 디렉토리로 이동
cd frontend

# 패키지 설치
npm install

# 개발 서버 실행 (http://localhost:5173)
npm run dev

# 프론트엔드 빌드
npm run build

# 타입 체크
npm run build
```

## 아키텍처 개요

### 헥사고날 아키텍처 구조
```
com.baro13.readfast/
├── admin/                          # 도메인별 모듈
│   ├── authlog/                    # 인증 로그 도메인
│   │   ├── adapter/
│   │   │   ├── in/                 # 인바운드 어댑터
│   │   │   │   ├── controller/     # REST API 컨트롤러
│   │   │   │   └── scheduler/      # 스케줄러
│   │   │   └── out/                # 아웃바운드 어댑터
│   │   │       ├── archive/        # 아카이빙 스토리지
│   │   │       ├── batch/          # 배치 처리
│   │   │       └── db/             # 데이터베이스 액세스
│   │   ├── application/            # 애플리케이션 서비스
│   │   └── domain/                 # 도메인 모델 및 포트
│   └── policy/                     # 정책 관리 도메인
│       ├── adapter/
│       ├── application/
│       └── domain/
└── global/                         # 횡단 관심사
    ├── config/                     # 설정
    ├── datasource/                 # 데이터소스 라우팅
    ├── exception/                  # 예외 처리
    ├── logging/                    # 로깅
    ├── response/                   # API 응답
    └── validation/                 # 검증
```

### 주요 설계 패턴 및 기술 스택

#### 1. 헥사고날 아키텍처 (포트 & 어댑터)
- **인바운드 포트**: `AuthLogService`, `ArchivingService`, `PolicyManagementService`
- **아웃바운드 포트**: `AuthLogDbReader`, `AuthLogArchiveReader`, `DataRetentionPolicyRepository`
- **어댑터 구현**: JPA, QueryDSL, 파일 스토리지, 배치 처리
- 도메인 로직과 기술 구현의 완전한 분리

#### 2. 데이터 액세스 전략
- **V1 (오프셋)**: 전통적인 페이지네이션, 정확한 총 개수 제공
- **V2 (커서)**: 성능 최적화된 커서 기반 페이지네이션  
- **V3 (통합)**: DB + 아카이브 스토리지 통합 조회
- **Master-Slave**: 읽기/쓰기 분리를 통한 성능 최적화

#### 3. 고급 아카이빙 시스템
- **압축 전략**: Gzip, ZIP 등 다양한 압축 형식 지원
- **스토리지 추상화**: JSON, CSV, SQLite 등 다중 저장 형식
- **메타데이터 관리**: 파일 무결성, 체크섬, 압축률 추적
- **배치 처리**: Spring Batch 기반 대용량 데이터 처리

#### 4. 캐싱 및 성능 최적화
- **Redis 캐싱**: 정책 설정 및 자주 조회되는 데이터
- **Caffeine 로컬 캐시**: 애플리케이션 레벨 캐싱
- **QueryDSL**: 타입 안전한 동적 쿼리 및 조건 빌딩
- **배치 최적화**: Hibernate 배치 처리 및 JDBC 최적화

### 데이터베이스 설정
- **Master DB**: 3310 포트 (쓰기 전용)
- **Slave DB**: 3311 포트 (읽기 전용)
- **연결**: `jdbc:mysql://localhost:3310/read-fast` (Master), `jdbc:mysql://localhost:3311/read-fast` (Slave)
- **DDL**: Hibernate 자동 업데이트 활성화
- **성능 최적화**: 배치 처리, IN 절 패딩, 통계 비활성화
- **모니터링**: PMM (Percona Monitoring) - http://localhost:8081

### API 엔드포인트

#### 인증 로그 조회
- **V1**: `/api/v1/auth/search` - 오프셋 기반 페이지네이션 (DB 전용)
- **V2**: `/api/v2/auth/search` - 커서 기반 페이지네이션 (DB 전용)
- **V3**: `/api/v3/auth/search` - 통합 조회 (DB + 아카이브 스토리지)
- **V3**: `/api/v3/auth/search-v2` - 통합 조회 커서 기반 (DB + 아카이브)

#### 아카이빙 관리
- **수동 실행**: `/api/v3/auth/archiving/execute` - 아카이빙 배치 수동 실행

#### 정책 관리
- **정책 조회**: `/api/v1/policies/{policyId}` - 데이터 보관 정책 조회
- **정책 업데이트**: `/api/v1/policies/{policyId}` - 정책 설정 업데이트

### 검색 기능
두 버전 모두 다음 조건으로 필터링 지원:
- 날짜 범위 (startDate, endDate)
- 인증 결과
- 디바이스 유형
- 사용자 ID
- 엔드포인트

V2는 추가로 `cursorDate`와 `cursorId` 파라미터를 사용한 커서 기반 페이지네이션을 지원합니다.

### 성능 모니터링
- 쿼리 성능 추적을 위한 커스텀 `@LogQueryTime` 어노테이션
- AOP 기반 로깅 애스펙트가 실행 시간 측정
- 비교를 위한 버전 식별자가 포함된 로그

### 엔티티 매핑
- **JPA 엔티티**: 데이터베이스 매핑을 가진 `AuthLogEntity`
- **도메인 객체**: 비즈니스 로직을 가진 `AuthLog`
- **매퍼**: 엔티티-도메인 변환을 위한 `AuthLogMapper`

## 개발 참고사항

### 새로운 쿼리 기능 추가
1. `AuthSearchCondition` DTO 업데이트
2. QueryDSL 리포지토리 클래스에 조건 메서드 추가
3. V1과 V2 구현 모두에 대한 영향 고려

### 성능 고려사항
- V1 쿼리는 총 페이지 수를 위한 count 쿼리 실행
- V2는 더 빠른 응답 시간을 위해 count 쿼리 건너뛰기
- V2의 커서 페이지네이션은 대용량 데이터셋에서 더 효율적

### 테스트
- 메인 테스트 클래스: `ReadFastApplicationTests`
- 테스트 프로파일은 로컬 프로파일과 별도로 구성 가능
- 데이터베이스 테스트는 격리를 위해 테스트 컨테이너나 H2 사용 권장

### CORS 설정
- 프론트엔드 허용 주소: `http://localhost:5173`
- 전체 CRUD 작업 허용
- 인증된 요청을 위한 자격 증명 허용

## 엔터프라이즈 데이터 보관 정책 시스템

### 개요
고급 데이터 라이프사이클 관리 시스템으로, 성능 최적화와 규정 준수를 위한 자동화된 아카이빙 솔루션을 제공합니다. 압축, 암호화, 다중 스토리지 지원 및 무결성 검증 기능을 포함합니다.

### 고급 설정 옵션
```yaml
# 데이터 보관 정책 설정
data:
  retention:
    db-retention-days: 90        # DB에 보관할 일수
    total-retention-days: 365    # 전체 보관 일수
    batch-size: 1000             # 배치 처리 크기
    archive-base-path: "/Users/seongmin/project/claude/ReadFast/archive-data"
    archive-file-format: "yyyy-MM-dd"           # 아카이브 파일명 형식
    enable-archiving: true       # 아카이빙 활성화
    enable-data-deletion: true   # 데이터 삭제 활성화
    cron-expression: "0 0 2 * * ?"  # 매일 새벽 2시 실행
    
    # 압축 및 저장 옵션
    enable-compression: true     # 압축 활성화
    compression-format: "gzip"   # 압축 형식 (gzip, zip, none)
    archive-data-format: "csv"   # 저장 형식 (json, csv, parquet)
    
    # SQLite 분석 DB 설정
    enable-sqlite-conversion: true              # SQLite 변환 활성화
    sqlite-cleanup-after-conversion: false     # 변환 후 압축 파일 삭제 여부

# 배치 처리 설정
batch:
  thread-pool:
    core-pool-size: 4           # 기본 스레드 풀 크기
    max-pool-size: 8            # 최대 스레드 풀 크기
    queue-capacity: 100         # 작업 큐 용량
    thread-name-prefix: "batch-"
    keep-alive-seconds: 60
    await-termination-seconds: 30
```

### 엔터프라이즈 아키텍처 구성요소

#### 1. 고급 배치 처리 시스템 (Spring Batch)
- **BatchCoordinator**: 멀티 스테이지 배치 작업 오케스트레이션
- **ChunkProcessor**: 대용량 데이터 청크 단위 병렬 처리
- **BatchProgressTracker**: 실시간 배치 진행률 및 성능 모니터링
- **ArchiveMetadataManager**: 아카이브 메타데이터 생성 및 관리

#### 2. 다중 스토리지 추상화 계층
- **StorageFactory**: 스토리지 타입별 팩토리 패턴 구현
- **JsonStorage**: JSON 형태 아카이브 저장
- **CsvStorage**: CSV 형태 아카이브 저장 (Metabase 호환)
- **SqliteStorage**: SQLite 분석 DB 생성 및 관리
- **AbstractStorage**: 공통 스토리지 기능 추상화

#### 3. 압축 시스템
- **CompressionFactory**: 압축 알고리즘 팩토리
- **GzipCompression**: Gzip 압축 구현
- **NoCompression**: 압축 없음 옵션

#### 4. 통합 조회 및 라우팅
- **AuthLogService**: DB와 아카이브 통합 조회 서비스
- **ArchivingService**: 아카이빙 작업 관리 및 실행
- **DataRetentionPolicyProvider**: 동적 정책 제공 및 캐싱

#### 5. 모니터링 및 스케줄링
- **ArchivingScheduler**: 정책 기반 자동 스케줄링
- **ArchivePathCalculator**: 날짜 기반 경로 계산
- **PolicyCache**: Redis 기반 정책 캐싱

### 사용 방법

#### 1. 일반 조회 (기존 방식)
```bash
# V1 - DB에서만 조회
GET /api/v1/auth/search?startDate=2024-01-01T00:00:00Z&endDate=2024-12-31T23:59:59Z

# V2 - DB에서만 조회 (커서 기반)
GET /api/v2/auth/search?startDate=2024-01-01T00:00:00Z&endDate=2024-12-31T23:59:59Z
```

#### 2. 통합 조회 (DB + 스토리지)
```bash
# V3 - 통합 조회 (자동으로 DB와 스토리지에서 조회)
GET /api/v3/auth/search?startDate=2024-01-01T00:00:00Z&endDate=2024-12-31T23:59:59Z

# V3 - 통합 조회 V2 (커서 기반)
GET /api/v3/auth/search-v2?startDate=2024-01-01T00:00:00Z&endDate=2024-12-31T23:59:59Z
```

### 엔터프라이즈 확장성 및 운영 고려사항

#### 1. 클라우드 네이티브 스토리지 지원
- **현재 구현**: 로컬 파일 시스템 기반 다중 형식 저장
- **확장 가능**: AWS S3, Azure Blob, Google Cloud Storage 어댑터 추가 가능
- **하이브리드 구조**: 온프레미스와 클라우드 스토리지 동시 지원
- **Storage 팩토리 패턴**: 새로운 스토리지 구현체 플러그인 방식으로 추가

#### 2. 고성능 처리 및 최적화
- **청크 기반 병렬 처리**: 대용량 데이터 멀티 스레드 처리
- **압축 최적화**: 저장 공간 60-80% 절약
- **인텔리전트 라우팅**: 조회 패턴에 따른 최적 데이터 소스 선택
- **캐싱 계층**: Redis + Caffeine 다중 레벨 캐싱

#### 3. 운영 모니터링 및 관리
- **실시간 대시보드**: 배치 실행 상태, 처리량, 성능 메트릭
- **무결성 검증**: 체크섬 기반 데이터 무결성 자동 검증
- **자동 복구**: 실패한 아카이빙 작업 자동 재시도 및 알림
- **성능 추적**: @LogQueryTime 어노테이션 기반 성능 모니터링

#### 4. 규정 준수 및 거버넌스
- **데이터 라이프사이클**: 자동화된 데이터 보관 및 삭제 정책
- **감사 추적**: 모든 아카이빙 작업 이력 추적 및 보고
- **암호화 지원**: 저장 데이터 암호화 및 전송 중 암호화
- **정책 기반 관리**: 동적 정책 변경 및 즉시 적용

### 운영 가이드

#### 1. 아카이빙 배치 수동 실행
```bash
# 배치 수동 실행 (필요시)
# DataArchivingScheduler.executeManualArchiving() 메서드 호출
```

#### 2. 스토리지 경로 설정
```bash
# 아카이브 디렉토리 생성
mkdir -p /tmp/readfast/archive

# 권한 설정
chmod 755 /tmp/readfast/archive
```

#### 3. 모니터링 포인트
- 배치 실행 주기와 성공률
- 아카이빙된 데이터 크기와 개수
- 스토리지 용량 사용률
- 통합 조회 성능 및 응답 시간

## 엔터프라이즈 관리 대시보드 (React + TypeScript)

### 개요
React + TypeScript + Vite 기반의 현대적인 관리 대시보드로, 인증 로그 조회, 아카이빙 관리, 정책 설정을 위한 통합 관리 인터페이스를 제공합니다. API 성능 비교, 실시간 모니터링, 정책 시뮬레이션 등 고급 관리 기능을 포함합니다.

### 핵심 기능 및 모듈

#### 1. 통합 대시보드 구조
```
frontend/src/
├── pages/
│   ├── AuthLogs/           # 인증 로그 조회 및 성능 비교
│   ├── Archiving/          # 아카이빙 관리 대시보드
│   ├── Settings/           # 정책 설정 및 관리
│   └── Dashboard/          # 메인 대시보드 (통계 및 모니터링)
├── services/
│   ├── authLogService.ts   # 인증 로그 API 서비스
│   ├── archivingService.ts # 아카이빙 관리 API 서비스
│   ├── policyService.ts    # 정책 관리 API 서비스
│   └── apiResponseAdapter.ts # 백엔드-프론트엔드 API 어댑터
├── hooks/
│   ├── useAuthLogs.ts      # 인증 로그 상태 관리
│   ├── useArchiving.ts     # 아카이빙 상태 관리
│   └── usePagination.ts    # 페이지네이션 로직
└── components/ui/          # 재사용 가능한 UI 컴포넌트
```

#### 2. API 성능 비교 시스템
- **V1 (오프셋)**: 전통적인 페이지네이션, 정확한 총 개수 제공
- **V2 (커서)**: 성능 최적화된 커서 기반 페이지네이션
- **V3 (통합)**: DB + 아카이브 스토리지 통합 조회
- **실시간 메트릭**: 응답 시간, 처리량, 메모리 사용량
- **성능 등급**: 우수(<100ms), 보통(<500ms), 개선 필요(≥500ms)

#### 3. 아카이빙 관리 인터페이스
- **실시간 배치 모니터링**: 진행률, 처리 속도, 남은 시간
- **아카이브 메타데이터 관리**: 파일 목록, 무결성 상태, 압축률
- **스토리지 사용량 추적**: 용량 사용률, 파일 분포, 증가 추세
- **수동 작업 제어**: 배치 실행, 일시 중지, 재개, 중단

#### 4. 정책 설정 및 시뮬레이션
- **동적 정책 편집**: 실시간 유효성 검증 및 미리보기
- **영향도 시뮬레이션**: 정책 변경 시 예상 영향도 계산
- **최적화 제안**: AI 기반 정책 최적화 권장사항
- **규정 준수 체크**: 데이터 보관 규정 준수 상태 검증

### 기술 스택 및 아키텍처

#### 프론트엔드 기술 스택
- **React 18**: 함수형 컴포넌트 및 Hooks 기반
- **TypeScript**: 타입 안전성 및 개발 생산성
- **Vite**: 빠른 개발 서버 및 빌드 시스템
- **TanStack Query**: 서버 상태 관리 및 캐싱
- **React Hook Form + Zod**: 폼 상태 관리 및 유효성 검증
- **TailwindCSS**: 유틸리티 우선 CSS 프레임워크
- **Lucide React**: 일관된 아이콘 시스템

#### 주요 타입 시스템
```typescript
// API 성능 메트릭
interface ApiPerformanceMetrics {
  version: 'v1' | 'v2' | 'v3';
  responseTime: number;
  totalRecords: number;
  timestamp: Date;
  memoryUsage?: number;
  cacheHitRate?: number;
}

// 아카이빙 상태
interface ArchivingStatus {
  isRunning: boolean;
  progress: number;
  currentStage: string;
  estimatedTimeRemaining: number;
  processedRecords: number;
  totalRecords: number;
}

// 정책 설정
interface DataRetentionPolicy {
  id: number;
  name: string;
  dbRetentionDays: number;
  totalRetentionDays: number;
  enableCompression: boolean;
  compressionFormat: 'gzip' | 'zip' | 'none';
  archiveDataFormat: 'json' | 'csv' | 'parquet';
}
```

### 프록시 설정
```javascript
// vite.config.ts - 프론트엔드에서 백엔드 API 프록시 설정
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
    },
  },
})
```

### 통합 테스트 확인사항 ✅
1. **백엔드 API**: 모든 버전(V1/V2/V3) 정상 작동
2. **프론트엔드 빌드**: TypeScript 컴파일 에러 없음
3. **데이터 플로우**: 백엔드 → 어댑터 → 프론트엔드 완전 연동
4. **성능 측정**: 실시간 응답 시간 측정 및 비교 대시보드
5. **CORS 설정**: 프론트엔드(5173) ↔ 백엔드(8080) 연동

### 개발 워크플로우
```bash
# 1. 백엔드 실행
./gradlew bootRun

# 2. 프론트엔드 실행
cd frontend && npm run dev

# 3. 브라우저에서 확인
http://localhost:5173

# 4. API 직접 테스트
curl "http://localhost:8080/api/v1/auth/search?page=0&size=5"
```

### 중요 설계 결정사항
1. **어댑터 패턴**: 백엔드 API 변경 없이 프론트엔드 통합
2. **타입 안전성**: TypeScript로 런타임 에러 방지
3. **성능 우선**: 캐싱, 배치 요청, 효율적인 상태 관리
4. **확장성**: 새로운 API 버전 추가 용이한 구조
5. **사용자 경험**: 실시간 성능 비교 및 직관적인 UI

### 향후 확장 계획

#### 1. 고급 분석 및 인사이트
- [ ] 실시간 데이터 스트리밍 (WebSocket)
- [ ] 머신러닝 기반 이상 탐지
- [ ] 예측 분석 및 트렌드 예측
- [ ] 대화형 데이터 시각화 (Chart.js, D3.js)

#### 2. 엔터프라이즈 기능
- [ ] 역할 기반 접근 제어 (RBAC)
- [ ] SSO (Single Sign-On) 통합
- [ ] 다중 테넌시 지원
- [ ] API 키 관리 및 rate limiting

#### 3. 운영 효율성
- [ ] 자동화된 알림 시스템
- [ ] 스마트 정책 추천 엔진
- [ ] 성능 최적화 자동 제안
- [ ] 장애 복구 자동화

#### 4. 글로벌 지원
- [ ] 다국어 지원 (i18n)
- [ ] 지역별 규정 준수
- [ ] 시간대 자동 변환
- [ ] 지역별 데이터 센터 지원

## 추가 개발 도구 및 스크립트

### 데이터 생성 및 테스트
```bash
# Python 기반 대용량 테스트 데이터 생성
cd scripts
./setup_python.sh                    # Python 환경 설정
python generate_test_data.py         # 테스트 데이터 생성
./generate_large_dataset.sh          # 대용량 데이터셋 생성

# 자동화 스크립트 실행
./run_scripts.sh                     # 통합 실행 스크립트
```

### 프로젝트 구조 요약
- **엔터프라이즈급 Spring Boot 3.5** 기반 백엔드
- **헥사고날 아키텍처** 및 DDD 패턴 적용
- **Master-Slave MySQL** + **Redis 캐싱** + **PMM 모니터링**
- **Spring Batch** 기반 고급 아카이빙 시스템
- **React 18 + TypeScript** 관리 대시보드
- **다중 스토리지** 및 **압축** 지원
- **실시간 성능 모니터링** 및 **정책 관리**

### 핵심 개발 원칙
1. **도메인 주도 설계**: 비즈니스 로직과 기술 구현 분리
2. **성능 우선**: 캐싱, 배치 처리, 인덱싱 최적화
3. **확장성**: 클라우드 네이티브 및 마이크로서비스 준비
4. **운영성**: 모니터링, 로깅, 자동화 내장
5. **보안**: 데이터 암호화, 접근 제어, 감사 추적