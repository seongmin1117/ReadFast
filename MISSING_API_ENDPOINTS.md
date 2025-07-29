# Missing API Endpoints - 백엔드 구현 필요

이 문서는 프론트엔드에서 호출하고 있지만 백엔드에 아직 구현되지 않은 API 엔드포인트들을 정리합니다.

## 현재 구현된 엔드포인트

✅ **구현 완료된 엔드포인트**:
- `GET /api/v3/auth/archiving/execute` - 수동 아카이빙 배치 실행
- `POST /api/v3/auth/archiving/execute-by-date` - 날짜 지정 아카이빙 배치 실행
- `GET /api/v3/auth/archiving/status` - 아카이빙 상태 조회
- `GET /api/v1/policies/{policyId}` - 정책 조회
- `PUT /api/v1/policies/{policyId}` - 정책 업데이트
- `GET /api/admin/cache/statistics` - 캐시 통계 조회
- `GET /api/admin/cache/health` - 캐시 상태 조회
- `GET /api/admin/datasource/status` - 데이터소스 상태 조회

## 구현 필요한 엔드포인트

### 1. 아카이브 메타데이터 관리
❌ **구현 필요**: `GET /api/v3/auth/archiving/metadata`
- **목적**: 아카이브 파일 메타데이터 목록 조회
- **매개변수**: page, size, startDate, endDate, status
- **응답**: ArchiveMetadata[] 페이지네이션 결과
- **현재 상태**: 프론트엔드에서 빈 결과 반환 중

### 2. 아카이빙 통계
❌ **구현 필요**: `GET /api/v3/auth/archiving/stats`
- **목적**: 아카이빙 성능 및 통계 정보 조회
- **매개변수**: period (선택사항)
- **응답**: 압축률, 처리 건수, 평균 실행 시간 등
- **현재 상태**: 프론트엔드에서 기본값 반환 중

### 3. 스토리지 사용량
❌ **구현 필요**: `GET /api/v3/auth/archiving/storage/usage`
- **목적**: 아카이브 스토리지 사용량 조회
- **응답**: 총 용량, 사용량, 사용률, 파일 수 등
- **현재 상태**: 프론트엔드에서 모의 데이터 반환 중

### 4. 아카이빙 제어
❌ **구현 필요**: `POST /api/v3/auth/archiving/pause`
- **목적**: 실행 중인 아카이빙 작업 일시 중지
- **현재 상태**: 프론트엔드에서 에러 발생

❌ **구현 필요**: `POST /api/v3/auth/archiving/resume`
- **목적**: 일시 중지된 아카이빙 작업 재개
- **현재 상태**: 프론트엔드에서 에러 발생

### 5. 아카이브 파일 관리
❌ **구현 필요**: `GET /api/v3/auth/archiving/download/{archiveId}`
- **목적**: 특정 아카이브 파일 다운로드
- **응답**: 바이너리 파일 데이터

❌ **구현 필요**: `POST /api/v3/auth/archiving/verify/{archiveId}`
- **목적**: 아카이브 파일 무결성 검증

❌ **구현 필요**: `DELETE /api/v3/auth/archiving/{archiveId}`
- **목적**: 아카이브 파일 삭제

### 6. 실행 히스토리
❌ **구현 필요**: `GET /api/v3/auth/archiving/history`
- **목적**: 아카이빙 실행 히스토리 조회
- **매개변수**: page, size, startDate, endDate, status
- **응답**: PolicyExecutionResult[] 페이지네이션 결과

### 7. 데이터 복원
❌ **구현 필요**: `POST /api/v3/auth/archiving/restore/{archiveId}`
- **목적**: 아카이브에서 데이터 복원
- **매개변수**: targetDate, restoreLocation 등

### 8. 시스템 최적화
❌ **구현 필요**: `GET /api/v3/auth/archiving/optimize`
- **목적**: 아카이빙 성능 최적화 정보 조회

## 임시 해결 방안

현재 프론트엔드에서는 다음과 같은 임시 방안을 사용하고 있습니다:

1. **콘솔 경고 출력**: 구현되지 않은 API 호출 시 경고 메시지
2. **기본값 반환**: 통계나 메타데이터의 경우 빈 값이나 기본값 반환
3. **적절한 에러 처리**: 제어 API의 경우 사용자에게 명확한 에러 메시지 표시

## 우선순위 권장사항

### 높은 우선순위 (사용자 경험에 직접적 영향)
1. `GET /api/v3/auth/archiving/stats` - 대시보드 통계 표시
2. `GET /api/v3/auth/archiving/storage/usage` - 스토리지 모니터링
3. `GET /api/v3/auth/archiving/metadata` - 아카이브 파일 목록

### 중간 우선순위 (관리 기능)
4. `GET /api/v3/auth/archiving/history` - 실행 히스토리
5. `POST /api/v3/auth/archiving/verify/{archiveId}` - 파일 검증
6. `GET /api/v3/auth/archiving/download/{archiveId}` - 파일 다운로드

### 낮은 우선순위 (고급 기능)
7. `POST /api/v3/auth/archiving/pause/resume` - 작업 제어
8. `POST /api/v3/auth/archiving/restore/{archiveId}` - 데이터 복원
9. `DELETE /api/v3/auth/archiving/{archiveId}` - 파일 삭제
10. `GET /api/v3/auth/archiving/optimize` - 성능 최적화

## 다음 단계

1. **백엔드 개발팀과 협의**: API 스펙 확정 및 구현 일정 논의
2. **점진적 구현**: 우선순위에 따른 단계별 구현
3. **프론트엔드 업데이트**: API 구현 완료 시 프론트엔드 코드에서 임시 방안 제거
4. **테스트 및 검증**: 각 API 구현 후 전체 플로우 테스트

---

📝 **마지막 업데이트**: 2025-07-29
🚧 **상태**: 분석 완료, 백엔드 구현 대기 중