# ReadFast API 명세서

## 개요
ReadFast는 인증 로그 조회 및 데이터 보관 정책 관리를 위한 REST API를 제공합니다.
이 문서는 현재 구현된 API와 프론트엔드에서 요구하는 API를 포함한 완전한 명세를 제공합니다.

## 기본 설정
- **Base URL**: `http://localhost:8080/api`
- **Content-Type**: `application/json`
- **CORS**: `http://localhost:5173` (프론트엔드)

## 응답 형식
모든 API는 다음과 같은 공통 응답 형식을 사용합니다:

```json
{
  "dateTime": "2024-01-01T10:00:00",
  "internalCode": "SUCCESS",
  "internalCodeDescription": "성공",
  "data": {}
}
```

---

## 1. 인증 로그 API

### 1.1 V1 인증 로그 조회 (오프셋 기반 페이지네이션)
- **URL**: `/v1/auth/search`
- **Method**: `GET`
- **구현 상태**: ✅ 백엔드 구현완료 / ✅ 프론트엔드 구현완료

**요청 파라미터**:
```typescript
interface V1SearchParams {
  page?: number;           // 페이지 번호 (기본값: 0)
  size?: number;           // 페이지 크기 (기본값: 10)
  startDate?: string;      // 시작일 (ISO 8601)
  endDate?: string;        // 종료일 (ISO 8601)
  authResult?: 'SUCCESS' | 'FAIL';
  deviceType?: string;
  userId?: string;
  endpoint?: string;
}
```

**응답**:
```typescript
interface V1SearchResponse {
  content: AuthLog[];
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
  number: number;
  size: number;
}
```

### 1.2 V2 인증 로그 조회 (커서 기반 페이지네이션)
- **URL**: `/v2/auth/search`
- **Method**: `GET`
- **구현 상태**: ✅ 백엔드 구현완료 / ✅ 프론트엔드 구현완료

**요청 파라미터**:
```typescript
interface V2SearchParams {
  size?: number;           // 페이지 크기 (기본값: 10)
  cursorDate?: string;     // 커서 날짜 (ISO 8601)
  cursorId?: number;       // 커서 ID
  startDate?: string;
  endDate?: string;
  authResult?: 'SUCCESS' | 'FAIL';
  deviceType?: string;
  userId?: string;
  endpoint?: string;
}
```

**응답**:
```typescript
interface V2SearchResponse {
  content: AuthLog[];
  hasNext: boolean;
  nextCursorDate?: string;
  nextCursorId?: number;
}
```

### 1.3 V3 통합 조회 (DB + 스토리지)
- **URL**: `/v3/auth/search`
- **Method**: `GET`
- **구현 상태**: ✅ 백엔드 구현완료 / ✅ 프론트엔드 구현완료

**요청 파라미터**: V1과 동일

**응답**: V1과 동일하지만 DB와 스토리지의 통합 결과

### 1.4 V3 통합 조회 V2 (커서 기반)
- **URL**: `/v3/auth/search-v2`
- **Method**: `GET`
- **구현 상태**: ✅ 백엔드 구현완료 / ✅ 프론트엔드 구현완료

**요청 파라미터**: V2와 동일

**응답**: V2와 동일하지만 DB와 스토리지의 통합 결과

---

## 2. 데이터 보관 정책 API

### 2.1 정책 조회
- **URL**: `/v1/policies/{policyId}`
- **Method**: `GET`
- **구현 상태**: ✅ 백엔드 구현완료 / ✅ 프론트엔드 구현완료

**응답**:
```typescript
interface DataRetentionPolicy {
  id: number;
  name: string;
  dbRetentionDays: number;
  totalRetentionDays: number;
  batchSize: number;
  archiveBasePath: string;
  archiveFileFormat: string;
  enableArchiving: boolean;
  enableDataDeletion: boolean;
  cronExpression: string;
  createdAt: string;
  updatedAt: string;
}
```

### 2.2 정책 업데이트
- **URL**: `/v1/policies/{policyId}`
- **Method**: `PUT`
- **구현 상태**: ✅ 백엔드 구현완료 / ✅ 프론트엔드 구현완료

**요청 본문**:
```typescript
interface UpdatePolicyRequest {
  name?: string;
  dbRetentionDays?: number;
  totalRetentionDays?: number;
  batchSize?: number;
  archiveBasePath?: string;
  archiveFileFormat?: string;
  enableArchiving?: boolean;
  enableDataDeletion?: boolean;
  cronExpression?: string;
}
```

### 2.3 정책 검증 (필요 - 미구현)
- **URL**: `/v1/policies/validate`
- **Method**: `POST`
- **구현 상태**: ❌ 백엔드 미구현 / ✅ 프론트엔드 구현완료 (클라이언트 측 검증)

**요청 본문**: `UpdatePolicyRequest`와 동일

**응답**:
```typescript
interface PolicyValidationResponse {
  isValid: boolean;
  errors: string[];
  warnings: string[];
}
```

### 2.4 정책 적용 시뮬레이션 (필요 - 미구현)
- **URL**: `/v1/policies/simulate`
- **Method**: `POST`
- **구현 상태**: ❌ 백엔드 미구현 / ✅ 프론트엔드 구현완료 (기본값 반환)

**요청 본문**: `UpdatePolicyRequest`와 동일

**응답**:
```typescript
interface PolicySimulationResponse {
  estimatedAffectedRecords: number;
  estimatedArchiveSize: number;
  estimatedExecutionTime: number;
  storageRequirement: number;
  potentialIssues: string[];
}
```

---

## 3. 아카이빙 관리 API

### 3.1 수동 아카이빙 실행
- **URL**: `/v3/auth/archiving/execute`
- **Method**: `GET`
- **구현 상태**: ❌ 백엔드 미구현 / ✅ 프론트엔드 구현완료

**응답**:
```typescript
interface ArchivingResult {
  success: boolean;
  processedRecords: number;
  durationMillis: number;
  message?: string;
  archiveFiles?: string[];
  errors?: string[];
}
```

### 3.2 아카이빙 상태 조회
- **URL**: `/v3/auth/archiving/status`
- **Method**: `GET`
- **구현 상태**: ❌ 백엔드 미구현 / ✅ 프론트엔드 구현완료 (기본값 반환)

**응답**:
```typescript
interface ArchivingStatus {
  isRunning: boolean;
  lastExecution?: PolicyExecutionResult;
  nextScheduledTime?: string;
  queueSize: number;
}
```

### 3.3 아카이빙 일시 중지/재개
- **URL**: `/v3/auth/archiving/pause` | `/v3/auth/archiving/resume`
- **Method**: `POST`
- **구현 상태**: ❌ 백엔드 미구현 / ✅ 프론트엔드 구현완료

### 3.4 아카이브 메타데이터 목록 조회
- **URL**: `/v3/auth/archiving/metadata`
- **Method**: `GET`
- **구현 상태**: ❌ 백엔드 미구현 / ✅ 프론트엔드 구현완료 (빈 결과 반환)

**요청 파라미터**:
```typescript
interface ArchiveMetadataParams {
  page?: number;
  size?: number;
  startDate?: string;
  endDate?: string;
  status?: 'CREATED' | 'VERIFIED' | 'CORRUPTED' | 'DELETED';
}
```

**응답**:
```typescript
interface ArchiveMetadataResponse {
  content: ArchiveMetadata[];
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

interface ArchiveMetadata {
  id: number;
  fileName: string;
  filePath: string;
  fileSize: number;
  recordCount: number;
  checksum: string;
  compressionRatio: number;
  status: 'CREATED' | 'VERIFIED' | 'CORRUPTED' | 'DELETED';
  archiveDate: string;
  startDate: string;
  endDate: string;
  createdAt: string;
  lastVerified?: string;
}
```

### 3.5 아카이브 파일 다운로드
- **URL**: `/v3/auth/archiving/download/{archiveId}`
- **Method**: `GET`
- **구현 상태**: ❌ 백엔드 미구현 / ✅ 프론트엔드 구현완료

**응답**: Binary (파일 다운로드)

### 3.6 아카이브 파일 검증
- **URL**: `/v3/auth/archiving/verify/{archiveId}`
- **Method**: `POST`
- **구현 상태**: ❌ 백엔드 미구현 / ✅ 프론트엔드 구현완료

**응답**:
```typescript
interface ArchiveVerificationResult {
  isValid: boolean;
  checksum: string;
  recordCount: number;
  issues?: string[];
}
```

### 3.7 아카이브 파일 삭제
- **URL**: `/v3/auth/archiving/{archiveId}`
- **Method**: `DELETE`
- **구현 상태**: ❌ 백엔드 미구현 / ✅ 프론트엔드 구현완료

### 3.8 아카이빙 실행 히스토리 조회
- **URL**: `/v3/auth/archiving/history`
- **Method**: `GET`
- **구현 상태**: ❌ 백엔드 미구현 / ✅ 프론트엔드 구현완료 (빈 결과 반환)

**요청 파라미터**:
```typescript
interface ExecutionHistoryParams {
  page?: number;
  size?: number;
  startDate?: string;
  endDate?: string;
  status?: 'SUCCESS' | 'PARTIAL_SUCCESS' | 'FAILED';
}
```

**응답**:
```typescript
interface ExecutionHistoryResponse {
  content: PolicyExecutionResult[];
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

interface PolicyExecutionResult {
  id: number;
  executionDate: string;
  status: 'SUCCESS' | 'PARTIAL_SUCCESS' | 'FAILED';
  processedRecords: number;
  archivedRecords: number;
  deletedRecords: number;
  durationMillis: number;
  message?: string;
  errors?: string[];
}
```

### 3.9 아카이빙 통계 조회
- **URL**: `/v3/auth/archiving/stats`
- **Method**: `GET`
- **구현 상태**: ❌ 백엔드 미구현 / ✅ 프론트엔드 구현완료 (기본값 반환)

**요청 파라미터**:
```typescript
interface StatsParams {
  period?: 'day' | 'week' | 'month' | 'year';
}
```

**응답**:
```typescript
interface ArchivingStats {
  totalArchivedRecords: number;
  totalArchiveFiles: number;
  totalArchivedSize: number;
  compressionRatio: number;
  recentExecutions: number;
  failureRate: number;
  averageExecutionTime: number;
  oldestArchive?: string;
  newestArchive?: string;
}
```

### 3.10 스토리지 사용량 조회
- **URL**: `/v3/auth/archiving/storage/usage`
- **Method**: `GET`
- **구현 상태**: ❌ 백엔드 미구현 / ✅ 프론트엔드 구현완료 (기본값 반환)

**응답**:
```typescript
interface StorageUsage {
  totalSize: number;
  usedSize: number;
  availableSize: number;
  usagePercentage: number;
  fileCount: number;
  storageType: string;
  lastUpdated: string;
}
```

### 3.11 아카이브 무결성 검사
- **URL**: `/v3/auth/archiving/integrity` | `/v3/auth/archiving/integrity/{archiveId}`
- **Method**: `GET`
- **구현 상태**: ❌ 백엔드 미구현 / ✅ 프론트엔드 구현완료

**응답**:
```typescript
interface IntegrityCheckResult {
  checkedFiles: number;
  corruptedFiles: number;
  missingFiles: number;
  details: Array<{
    archiveId: number;
    fileName: string;
    status: 'OK' | 'CORRUPTED' | 'MISSING';
    checksum: string;
    expectedChecksum: string;
  }>;
}
```

### 3.12 아카이빙 최적화 제안
- **URL**: `/v3/auth/archiving/optimize`
- **Method**: `GET`
- **구현 상태**: ❌ 백엔드 미구현 / ✅ 프론트엔드 구현완료 (기본 제안사항 반환)

**응답**:
```typescript
interface OptimizationSuggestions {
  suggestions: Array<{
    type: 'batch_size' | 'schedule' | 'retention' | 'compression';
    title: string;
    description: string;
    currentValue: any;
    suggestedValue: any;
    expectedImpact: string;
    priority: 'high' | 'medium' | 'low';
  }>;
  overallScore: number;
  lastAnalyzed: string;
}
```

### 3.13 아카이브에서 데이터 복원
- **URL**: `/v3/auth/archiving/restore/{archiveId}`
- **Method**: `POST`
- **구현 상태**: ❌ 백엔드 미구현 / ✅ 프론트엔드 구현완료

**요청 본문**:
```typescript
interface RestoreOptions {
  targetDate?: string;
  recordIds?: number[];
  restoreToTemp?: boolean;
}
```

**응답**:
```typescript
interface RestoreResult {
  success: boolean;
  restoredRecords: number;
  targetLocation: string;
  estimatedTime: number;
}
```

---

## 4. 공통 타입 정의

### AuthLog
```typescript
interface AuthLog {
  id: number;
  userId: string;
  endpoint: string;
  authResult: 'success' | 'failure';
  deviceType: string;
  ipAddress: string;
  timestamp: string;
  userAgent?: string;
  errorMessage?: string;
}
```

---

## 5. 구현 현황 요약

### ✅ 완전 구현 (백엔드 + 프론트엔드)
- V1/V2/V3 인증 로그 조회
- 데이터 보관 정책 조회/업데이트

### ⚠️ 프론트엔드만 구현 (백엔드 미구현)
- 정책 검증 API
- 정책 시뮬레이션 API
- 모든 아카이빙 관리 API (13개)

### 📋 우선순위 높은 미구현 API
1. **수동 아카이빙 실행** (`/v3/auth/archiving/execute`)
2. **아카이빙 상태 조회** (`/v3/auth/archiving/status`)  
3. **아카이빙 통계 조회** (`/v3/auth/archiving/stats`)
4. **스토리지 사용량 조회** (`/v3/auth/archiving/storage/usage`)
5. **정책 검증** (`/v1/policies/validate`)

### 📋 향후 구현 예정 API
- 아카이브 메타데이터 관리
- 파일 다운로드/업로드
- 무결성 검사
- 데이터 복원
- 최적화 제안

---

## 6. 개발 참고사항

### 인증 및 권한
현재 인증 시스템이 구현되어 있지 않으므로, 필요시 JWT 또는 세션 기반 인증을 추가해야 합니다.

### 에러 처리
모든 API는 다음과 같은 에러 응답 형식을 사용합니다:
```json
{
  "dateTime": "2024-01-01T10:00:00",
  "internalCode": "ERROR",
  "internalCodeDescription": "오류 메시지",
  "data": null
}
```

### 성능 고려사항
- V2 API (커서 기반)는 대용량 데이터에서 더 나은 성능을 제공
- V3 API는 DB와 스토리지를 통합하여 조회하므로 응답 시간이 더 클 수 있음
- 아카이빙 작업은 비동기로 처리되어야 함

### 확장성
- 정책 관리: 현재 단일 정책(ID=1)만 지원하지만, 향후 다중 정책 지원 가능
- 스토리지: 현재 로컬 파일 시스템만 지원하지만, S3 등 클라우드 스토리지 확장 가능
- 아카이빙: 현재 JSON 형태로 저장하지만, 압축 형식 및 암호화 지원 가능