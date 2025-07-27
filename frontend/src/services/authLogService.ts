import { ApiService, ApiCache } from './api';
import { ApiResponseAdapter } from './apiResponseAdapter';
import type { 
  AuthLog, 
  BackendAuthLog,
  AuthSearchCondition, 
  PageResponse,
  BackendPageResponse
} from '@/types/auth.types';
import type { BackendApiResponse } from '@/types/api.types';

export class AuthLogService {
  private static readonly CACHE_PREFIX = 'auth-logs';
  private static readonly CACHE_TTL = 300000; // 5분

  /**
   * 인증 로그 검색 (최적화된 통합 조회 API 사용)
   * 커서 기반 페이지네이션과 DB + 스토리지 통합 조회를 제공하는 최신 API
   */
  static async search(
    condition: AuthSearchCondition,
    useCache = false // 실시간 데이터이므로 기본적으로 캐시 비활성화
  ): Promise<PageResponse<AuthLog>> {
    const cacheKey = `${this.CACHE_PREFIX}-${JSON.stringify(condition)}`;
    
    if (useCache) {
      const cached = ApiCache.get<PageResponse<AuthLog>>(cacheKey);
      if (cached) {
        return cached;
      }
    }

    const params = this.buildSearchParams(condition);
    
    try {
      // 가장 최적화된 V3 search-v2 API 사용 (커서 기반 통합 조회)
      const response = await ApiService.get('/v3/auth/search-v2', params);
      const backendResponse = response as unknown as BackendApiResponse<BackendPageResponse<BackendAuthLog>>;
      const adaptedResponse = ApiResponseAdapter.adaptVersionedResponse(backendResponse);
      
      if (useCache && adaptedResponse.data) {
        ApiCache.set(cacheKey, adaptedResponse.data, this.CACHE_TTL);
      }

      return adaptedResponse.data;
    } catch (error) {
      console.error('인증 로그 검색 실패:', error);
      throw error;
    }
  }

  /**
   * 단일 로그 조회 (ID로 조회)
   */
  static async getById(id: number): Promise<AuthLog | null> {
    try {
      // 간단한 필터링으로 단일 로그 조회
      const searchCondition: AuthSearchCondition = {
        page: 0,
        size: 1,
        sortBy: 'date',
        direction: 'desc'
      };

      const response = await this.search(searchCondition, false);
      const found = response.content.find(log => log.id === id);
      
      return found || null;
    } catch (error) {
      console.error(`로그 조회 실패 (ID: ${id}):`, error);
      return null;
    }
  }

  /**
   * 검색 조건을 URL 파라미터로 변환
   */
  private static buildSearchParams(condition: AuthSearchCondition): Record<string, string> {
    const params: Record<string, string> = {};

    // 페이지네이션
    if (condition.page !== undefined) params.page = condition.page.toString();
    if (condition.size !== undefined) params.size = condition.size.toString();
    if (condition.sortBy) params.sortBy = condition.sortBy;
    if (condition.direction) params.direction = condition.direction;

    // 검색 조건
    if (condition.startDate) params.startDate = condition.startDate.toISOString();
    if (condition.endDate) params.endDate = condition.endDate.toISOString();
    if (condition.device) params.device = condition.device;
    if (condition.userId) params.userId = condition.userId;
    if (condition.result) params.result = condition.result;
    if (condition.endpoint) params.endpoint = condition.endpoint;

    // 커서 기반 페이지네이션 (V2 기능)
    if (condition.cursorId !== undefined) params.cursorId = condition.cursorId.toString();
    if (condition.cursorDate) params.cursorDate = condition.cursorDate.toISOString();

    return params;
  }

  /**
   * 검색 통계 정보 조회
   */
  static async getSearchStats(condition: AuthSearchCondition): Promise<SearchStats> {
    try {
      const response = await this.search(condition, false);
      
      return {
        totalElements: response.totalElements,
        totalPages: response.totalPages,
        currentPage: response.number,
        pageSize: response.size,
        hasNext: response.hasNext,
        hasPrevious: !response.first
      };
    } catch (error) {
      console.error('검색 통계 조회 실패:', error);
      return {
        totalElements: 0,
        totalPages: 0,
        currentPage: 0,
        pageSize: 0,
        hasNext: false,
        hasPrevious: false
      };
    }
  }

  /**
   * 캐시 무효화
   */
  static clearCache(): void {
    // AuthLog 관련 캐시만 제거 (패턴 매칭 사용)
    ApiCache.clear(`^${this.CACHE_PREFIX}`);
  }

  /**
   * 날짜 범위 유효성 검증
   */
  static validateDateRange(startDate?: Date, endDate?: Date): boolean {
    if (!startDate || !endDate) return true;
    
    const diffInDays = Math.ceil((endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24));
    
    // 최대 1년 범위까지만 허용
    return diffInDays <= 365 && diffInDays >= 0;
  }
}

// SearchStats 인터페이스가 없다면 추가
interface SearchStats {
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  hasNext: boolean;
  hasPrevious: boolean;
}