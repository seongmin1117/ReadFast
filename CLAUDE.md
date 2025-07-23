# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 저장소에서 작업할 때 필요한 가이드를 제공합니다.

## 프로젝트 개요

ReadFast는 세 가지 다른 검색 구현(V1, V2, V3)을 가진 인증 로그 조회용 Spring Boot 애플리케이션입니다. 이 애플리케이션은 페이지네이션과 검색 성능 최적화에 대한 다양한 접근법을 보여주며, React + TypeScript 기반의 프론트엔드 대시보드를 포함합니다.

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
# Docker를 통한 MySQL 데이터베이스 시작
docker-compose up -d

# 데이터베이스 중지
docker-compose down
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

### 패키지 구조
- **controller**: REST API 컨트롤러 (V1과 V2 버전)
- **application**: 비즈니스 로직을 가진 서비스 계층
- **domain**: 핵심 도메인 엔티티 (AuthLog)
- **infrastructure**: JPA와 QueryDSL 구현을 가진 데이터 액세스 계층
- **global**: 횡단 관심사 (로깅, API 응답)

### 주요 설계 패턴

#### 헥사고날 아키텍처
- **포트 (인터페이스)**: application 계층의 `AuthQueryRepository`
- **어댑터 (구현체)**: infrastructure 계층의 `AuthQueryRepositoryImpl`
- 비즈니스 로직과 데이터 액세스 간의 깨끗한 분리

#### 세 가지 쿼리 전략
- **V1**: 총 개수와 함께하는 전통적인 오프셋 기반 페이지네이션
- **V2**: 더 나은 성능을 위한 총 개수 없는 커서 기반 페이지네이션
- **V3**: DB + 스토리지 통합 조회 (아카이빙된 데이터 포함)

#### QueryDSL 통합
- 타입 안전한 조건으로 동적 쿼리 빌딩
- V1과 V2 구현을 위한 별도의 리포지토리 클래스
- JPA 엔티티에서 생성된 커스텀 Q-클래스

### 데이터베이스 설정
- **로컬 프로파일**: 3310 포트의 MySQL
- **연결**: `jdbc:mysql://localhost:3310/read-fast`
- **DDL**: Hibernate 자동 업데이트 활성화
- **로깅**: 로컬 프로파일에서 SQL 쿼리 로깅

### API 엔드포인트
- **V1**: `/api/v1/auth/search` - 오프셋 기반 페이지네이션
- **V2**: `/api/v2/auth/search` - 커서 기반 페이지네이션
- **V3**: `/api/v3/auth/search` - 통합 조회 (DB + 스토리지)
- **V3**: `/api/v3/auth/search-v2` - 통합 조회 V2 (DB + 스토리지)

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

## 데이터 보관 정책 (Data Retention Policy)

### 개요
시스템은 데이터베이스 성능 최적화를 위해 오래된 데이터를 파일로 아카이빙하고 필요시 조회할 수 있는 통합 조회 기능을 제공합니다.

### 설정 옵션
```yaml
data:
  retention:
    db-retention-days: 90        # DB에 보관할 일수 (기본값: 90일)
    total-retention-days: 365    # 전체 보관 일수 (기본값: 365일)
    batch-size: 1000             # 배치 처리 크기 (기본값: 1000)
    archive-base-path: "/tmp/readfast/archive"  # 아카이브 파일 저장 경로
    archive-file-format: "yyyy-MM-dd"           # 아카이브 파일명 형식
    enable-archiving: true       # 아카이빙 활성화 (기본값: true)
    enable-data-deletion: true   # 데이터 삭제 활성화 (기본값: true)
    cron-expression: "0 0 2 * * ?"  # 스케줄러 실행 시간 (기본값: 매일 새벽 2시)
```

### 아키텍처 구성요소

#### 1. 데이터 아카이빙 배치 (Spring Batch)
- **DataArchivingBatch**: 스프링 배치 작업으로 오래된 데이터를 아카이빙하고 삭제
- **두 단계 처리**:
  1. Archive Step: 오래된 데이터를 파일로 저장
  2. Cleanup Step: 아카이빙된 데이터를 DB에서 삭제

#### 2. 스토리지 추상화 계층
- **DataStorage**: 스토리지 구현체의 공통 인터페이스
- **LocalFileStorage**: 로컬 파일 시스템 구현체 (JSON 형태로 저장)
- **StorageService**: 여러 스토리지 구현체를 관리하는 서비스

#### 3. 통합 조회 서비스
- **IntegratedAuthQueryService**: DB와 스토리지를 통합하여 조회하는 서비스
- **자동 데이터 소스 선택**: 조회 기간에 따라 DB 또는 스토리지에서 조회

#### 4. 스케줄러
- **DataArchivingScheduler**: 설정된 시간에 아카이빙 배치 실행
- **수동 실행**: 필요시 수동으로 아카이빙 작업 실행 가능

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

### 확장성 고려사항

#### 1. 다양한 스토리지 지원
- **LocalFileStorage**: 로컬 파일 시스템
- **확장 가능**: S3, Azure Blob, Google Cloud Storage 등 추가 가능
- **인터페이스 기반**: 새로운 스토리지 구현체 쉽게 추가 가능

#### 2. 성능 최적화
- **배치 처리**: 대용량 데이터를 청크 단위로 처리
- **병렬 처리**: 여러 스토리지에 동시 저장 가능
- **조회 최적화**: 날짜 범위에 따른 효율적인 데이터 소스 선택

#### 3. 모니터링 및 로깅
- **배치 실행 상태**: 성공/실패 로그 기록
- **아카이빙 통계**: 처리된 레코드 수, 실행 시간 등
- **스토리지 상태**: 각 스토리지별 저장/조회 성공/실패 로그

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

## 프론트엔드 통합 (2025.07.23 완료)

### 개요
React + TypeScript + Vite 기반의 프론트엔드 대시보드가 Spring Boot 백엔드와 완전히 통합되었습니다. 백엔드-프론트엔드 간의 API 형식 불일치 문제를 해결하고, 세 가지 API 버전(V1/V2/V3)의 성능을 비교할 수 있는 대시보드를 구현했습니다.

### 핵심 기능

#### 1. API 응답 형식 어댑터 패턴
**문제**: 백엔드와 프론트엔드의 API 응답 형식 불일치
- **백엔드 형식**: `{dateTime, internalCode, internalCodeDescription, data}`
- **프론트엔드 기대 형식**: `{success, data, message, timestamp}`

**해결책**: `ApiResponseAdapter` 클래스 구현
```typescript
// 파일 위치: frontend/src/services/apiResponseAdapter.ts
export class ApiResponseAdapter {
  // 백엔드 API 응답을 프론트엔드 형식으로 변환
  static adaptApiResponse<T>(backendResponse: BackendApiResponse<T>): ApiResponse<T>
  
  // 백엔드 AuthLog를 프론트엔드 AuthLog로 변환
  static adaptAuthLog(backendAuthLog: BackendAuthLog): AuthLog
  
  // 백엔드 페이지 응답을 프론트엔드 페이지 응답으로 변환
  static adaptPageResponse(backendPageResponse: BackendPageResponse<BackendAuthLog>): PageResponse<AuthLog>
}
```

#### 2. 데이터 타입 변환
- **인증 결과**: "SUCCESS"/"FAIL" → "success"/"failure"
- **날짜 형식**: ISO 8601 문자열 처리
- **페이지네이션**: 완전한 페이지네이션 데이터 어댑팅

#### 3. API 버전 선택 및 성능 비교 대시보드
- **V1 (오프셋)**: 전통적인 페이지네이션, 정확한 총 개수 제공
- **V2 (커서)**: 성능 최적화된 커서 기반 페이지네이션
- **V3 (통합)**: DB + 스토리지 통합 조회
- **실시간 성능 측정**: 응답 시간, 총 레코드 수, 측정 시간 표시
- **성능 등급**: 우수(<100ms), 보통(<500ms), 개선 필요(≥500ms)

### 주요 구현 파일

#### 타입 정의
```bash
frontend/src/types/api.types.ts     # 백엔드 API 응답 타입
frontend/src/types/auth.types.ts    # 인증 로그 및 성능 메트릭 타입
```

#### 서비스 계층
```bash
frontend/src/services/apiResponseAdapter.ts  # API 응답 어댑터
frontend/src/services/authLogService.ts      # 성능 측정이 포함된 API 서비스
```

#### UI 컴포넌트
```bash
frontend/src/pages/AuthLogs/AuthLogList.tsx  # 메인 대시보드
frontend/src/hooks/useAuthLogs.ts            # 상태 관리 훅
```

#### 유틸리티
```bash
frontend/src/utils/format.utils.ts   # 데이터 포맷팅 유틸리티
frontend/src/utils/date.utils.ts     # 날짜 처리 유틸리티
```

### 성능 측정 기능
```typescript
interface ApiPerformanceMetrics {
  version: ApiVersion;
  responseTime: number;    // 밀리초
  totalRecords: number;    // 총 레코드 수
  timestamp: Date;         // 측정 시간
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

### 향후 작업 고려사항
- [ ] 더 많은 API 엔드포인트 통합
- [ ] 실시간 업데이트 (WebSocket)
- [ ] 고급 필터링 및 검색
- [ ] 데이터 시각화 (차트, 그래프)
- [ ] 사용자 권한 관리
- [ ] 다국어 지원