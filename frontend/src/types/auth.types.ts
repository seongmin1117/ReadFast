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
  startDate?: Date;
  endDate?: Date;
  device?: string;
  userId?: string;
  result?: string;
  endpoint?: string;
  page?: number;
  size?: number;
  sortBy?: 'id' | 'date' | 'device' | 'userId' | 'result' | 'endpoint';
  direction?: 'asc' | 'desc';
  // 커서 기반 페이지네이션
  cursorId?: number;
  cursorDate?: Date;
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
  number: number; // 현재 페이지 번호 (0부터 시작)
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean; // 마지막 페이지 여부
  first: boolean; // 첫 번째 페이지 여부
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

// 검색 결과 통계 (페이지네이션 기반)
export interface SearchStats {
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  hasNext: boolean;
  hasPrevious: boolean;
}