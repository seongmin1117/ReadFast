// 백엔드 실제 응답 형식
export interface BackendApiResponse<T = any> {
  dateTime: string;
  internalCode: number;
  internalCodeDescription: string;
  data: T;
}

// 프론트엔드 기대 응답 형식
export interface ApiResponse<T = any> {
  success: boolean;
  data: T;
  message?: string;
  timestamp?: string;
}

// API 에러 타입
export interface ApiError {
  status: number;
  code: string;
  message: string;
  details?: Record<string, any>;
}

// HTTP 메소드 타입
export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';

// API 엔드포인트 타입
export interface ApiEndpoint {
  url: string;
  method: HttpMethod;
  requiresAuth?: boolean;
}

// 요청 설정 타입
export interface RequestConfig {
  headers?: Record<string, string>;
  timeout?: number;
  retry?: number;
  cache?: boolean;
}

// 페이지네이션 파라미터
export interface PaginationParams {
  page: number;
  size: number;
  sort?: string;
  direction?: 'asc' | 'desc';
}

// 검색 파라미터 기본 인터페이스
export interface SearchParams {
  query?: string;
  filters?: Record<string, any>;
  pagination?: PaginationParams;
}

// API 호출 상태
export type ApiStatus = 'idle' | 'loading' | 'success' | 'error';

// 로딩 상태 관리
export interface LoadingState {
  isLoading: boolean;
  error: string | null;
  lastUpdated: Date | null;
}

// API 캐시 타입
export interface CacheEntry<T = any> {
  data: T;
  timestamp: number;
  expiry: number;
}