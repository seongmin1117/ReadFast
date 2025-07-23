// 백엔드에서 오는 실제 인증 로그 타입
export interface BackendAuthLog {
  id: number;
  date: string; // ISO 8601 format from backend
  device: string;
  userId: string;
  result: 'SUCCESS' | 'FAIL' | 'BLOCKED' | 'EXPIRED'; // 백엔드 실제 값
  endpoint: string;
}

// 프론트엔드 인증 로그 도메인 타입
export interface AuthLog {
  id: number;
  date: string;
  device: string;
  userId: string;
  result: 'success' | 'failure' | 'blocked' | 'expired'; // 프론트엔드 표준 값
  endpoint: string;
}

// 검색 조건 타입
export interface AuthSearchCondition {
  startDate?: string;
  endDate?: string;
  device?: string;
  userId?: string;
  result?: string;
  endpoint?: string;
  page?: number;
  size?: number;
  sortBy?: 'id' | 'date' | 'device' | 'userId' | 'result' | 'endpoint';
  direction?: 'asc' | 'desc';
  // V2 커서 기반 페이지네이션
  cursorId?: number;
  cursorDate?: string;
}

// 백엔드 페이지 응답 타입 (실제 형식)
export interface BackendPageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

// 프론트엔드 페이지 응답 타입
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

// 검색 필터 옵션
export interface SearchFilters {
  dateRange: {
    start: string | null;
    end: string | null;
  };
  device: string;
  userId: string;
  result: string;
  endpoint: string;
}

// 정렬 옵션
export interface SortOption {
  field: AuthSearchCondition['sortBy'];
  direction: AuthSearchCondition['direction'];
}

// 아카이빙 관련 타입
export interface ArchivingResult {
  success: boolean;
  processedRecords: number;
  durationMillis: number;
  message?: string;
}

// 검색 결과 통계
export interface SearchStats {
  totalRecords: number;
  filteredRecords: number;
  searchDuration: number;
  fromCache: boolean;
}

// API 버전 타입
export type ApiVersion = 'v1' | 'v2' | 'v3';

// API 성능 측정 결과
export interface ApiPerformanceMetrics {
  version: ApiVersion;
  responseTime: number; // 밀리초
  totalRecords: number;
  queryTime?: number; // 백엔드 쿼리 시간 (백엔드에서 제공하는 경우)
  timestamp: Date;
}

// 버전별 성능 비교 데이터
export interface VersionPerformanceComparison {
  v1?: ApiPerformanceMetrics;
  v2?: ApiPerformanceMetrics;
  v3?: ApiPerformanceMetrics;
}