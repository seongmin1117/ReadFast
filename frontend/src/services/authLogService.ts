import { ApiService, ApiCache } from './api';
import { ApiResponseAdapter } from './apiResponseAdapter';
import type { 
  AuthLog, 
  BackendAuthLog,
  AuthSearchCondition, 
  PageResponse,
  BackendPageResponse,
  SearchStats,
  ApiVersion,
  ApiPerformanceMetrics
} from '@/types/auth.types';
import type { BackendApiResponse } from '@/types/api.types';

export class AuthLogService {
  private static readonly CACHE_PREFIX = 'auth-logs';
  private static readonly CACHE_TTL = 300000; // 5분
  
  // 성능 측정 결과 저장
  private static performanceMetrics: Map<string, ApiPerformanceMetrics> = new Map();

  /**
   * 성능 측정을 위한 API 호출 래퍼
   */
  private static async measureApiCall<T>(
    version: ApiVersion,
    apiCall: () => Promise<T>,
    condition?: AuthSearchCondition
  ): Promise<{ result: T; metrics: ApiPerformanceMetrics }> {
    const startTime = Date.now();
    
    try {
      const result = await apiCall();
      const endTime = Date.now();
      
      const metrics: ApiPerformanceMetrics = {
        version,
        responseTime: endTime - startTime,
        totalRecords: (result as any)?.totalElements || 0,
        timestamp: new Date(endTime)
      };

      // 성능 메트릭 저장
      const metricsKey = `${version}-${JSON.stringify(condition || {})}`;
      this.performanceMetrics.set(metricsKey, metrics);

      return { result, metrics };
    } catch (error) {
      const endTime = Date.now();
      const metrics: ApiPerformanceMetrics = {
        version,
        responseTime: endTime - startTime,
        totalRecords: 0,
        timestamp: new Date(endTime)
      };

      throw { error, metrics };
    }
  }

  /**
   * 성능 메트릭 조회
   */
  static getPerformanceMetrics(version?: ApiVersion): ApiPerformanceMetrics[] {
    const metrics = Array.from(this.performanceMetrics.values());
    return version 
      ? metrics.filter(m => m.version === version)
      : metrics;
  }

  /**
   * 성능 메트릭 초기화
   */
  static clearPerformanceMetrics(): void {
    this.performanceMetrics.clear();
  }

  /**
   * V1 API - 오프셋 기반 페이지네이션
   */
  static async searchV1(
    condition: AuthSearchCondition,
    useCache = true
  ): Promise<{ data: PageResponse<AuthLog>; metrics: ApiPerformanceMetrics }> {
    const cacheKey = `${this.CACHE_PREFIX}-v1-${JSON.stringify(condition)}`;
    
    if (useCache) {
      const cached = ApiCache.get<PageResponse<AuthLog>>(cacheKey);
      if (cached) {
        // 캐시된 데이터의 경우 더미 메트릭 반환
        const dummyMetrics: ApiPerformanceMetrics = {
          version: 'v1',
          responseTime: 0,
          totalRecords: cached.totalElements,
          timestamp: new Date()
        };
        return { data: cached, metrics: dummyMetrics };
      }
    }

    const params = this.buildSearchParams(condition);
    
    const { result, metrics } = await this.measureApiCall(
      'v1',
      async () => {
        // ApiService.get은 ApiResponse<T>를 반환하지만, 실제로는 백엔드 응답 전체가 들어있음
        const response = await ApiService.get('/v1/auth/search', params);
        const backendResponse = response as unknown as BackendApiResponse<BackendPageResponse<BackendAuthLog>>;
        
        // 백엔드 응답을 프론트엔드 형식으로 변환
        const adaptedResponse = ApiResponseAdapter.adaptVersionedResponse(backendResponse, 'v1');
        
        return adaptedResponse.data;
      },
      condition
    );
    
    if (useCache && result) {
      ApiCache.set(cacheKey, result, this.CACHE_TTL);
    }

    return { data: result, metrics };
  }

  /**
   * V2 API - 커서 기반 페이지네이션
   */
  static async searchV2(
    condition: AuthSearchCondition,
    useCache = true
  ): Promise<{ data: PageResponse<AuthLog>; metrics: ApiPerformanceMetrics }> {
    const cacheKey = `${this.CACHE_PREFIX}-v2-${JSON.stringify(condition)}`;
    
    if (useCache) {
      const cached = ApiCache.get<PageResponse<AuthLog>>(cacheKey);
      if (cached) {
        const dummyMetrics: ApiPerformanceMetrics = {
          version: 'v2',
          responseTime: 0,
          totalRecords: cached.totalElements,
          timestamp: new Date()
        };
        return { data: cached, metrics: dummyMetrics };
      }
    }

    const params = this.buildSearchParams(condition);
    
    const { result, metrics } = await this.measureApiCall(
      'v2',
      async () => {
        const response = await ApiService.get('/v2/auth/search', params);
        const backendResponse = response as unknown as BackendApiResponse<BackendPageResponse<BackendAuthLog>>;
        
        const adaptedResponse = ApiResponseAdapter.adaptVersionedResponse(backendResponse, 'v2');
        
        return adaptedResponse.data;
      },
      condition
    );
    
    if (useCache && result) {
      ApiCache.set(cacheKey, result, this.CACHE_TTL);
    }

    return { data: result, metrics };
  }

  /**
   * V3 API - 통합 조회 (DB + 스토리지)
   */
  static async searchV3(
    condition: AuthSearchCondition,
    useCache = false // 실시간 데이터이므로 캐시 비활성화
  ): Promise<{ data: PageResponse<AuthLog>; metrics: ApiPerformanceMetrics }> {
    const cacheKey = `${this.CACHE_PREFIX}-v3-${JSON.stringify(condition)}`;
    
    if (useCache) {
      const cached = ApiCache.get<PageResponse<AuthLog>>(cacheKey);
      if (cached) {
        const dummyMetrics: ApiPerformanceMetrics = {
          version: 'v3',
          responseTime: 0,
          totalRecords: cached.totalElements,
          timestamp: new Date()
        };
        return { data: cached, metrics: dummyMetrics };
      }
    }

    const params = this.buildSearchParams(condition);
    
    const { result, metrics } = await this.measureApiCall(
      'v3',
      async () => {
        const response = await ApiService.get('/v3/auth/search', params);
        const backendResponse = response as unknown as BackendApiResponse<BackendPageResponse<BackendAuthLog>>;
        
        const adaptedResponse = ApiResponseAdapter.adaptVersionedResponse(backendResponse, 'v3');
        
        return adaptedResponse.data;
      },
      condition
    );
    
    if (useCache && result) {
      ApiCache.set(cacheKey, result, this.CACHE_TTL);
    }

    return { data: result, metrics };
  }

  /**
   * 기본 검색 (V3 사용)
   */
  static async search(
    condition: AuthSearchCondition,
    useCache = false
  ): Promise<{ data: PageResponse<AuthLog>; metrics: ApiPerformanceMetrics }> {
    return this.searchV3(condition, useCache);
  }

  /**
   * 버전별 검색 (통합 인터페이스)
   */
  static async searchByVersion(
    version: ApiVersion,
    condition: AuthSearchCondition,
    useCache = false
  ): Promise<{ data: PageResponse<AuthLog>; metrics: ApiPerformanceMetrics }> {
    switch (version) {
      case 'v1':
        return this.searchV1(condition, useCache);
      case 'v2':
        return this.searchV2(condition, useCache);
      case 'v3':
        return this.searchV3(condition, useCache);
      default:
        throw new Error(`Unsupported API version: ${version}`);
    }
  }

  /**
   * 단일 로그 조회 (ID로 조회)
   */
  static async getById(id: number): Promise<AuthLog | null> {
    try {
      // 간단한 필터링으로 단일 로그 조회
      const { data: response } = await this.search({
        page: 0,
        size: 1,
        sortBy: 'id',
        direction: 'desc',
      });

      const found = response.content.find(log => log.id === id);
      return found || null;
    } catch (error) {
      console.error('Failed to fetch auth log by ID:', error);
      return null;
    }
  }

  /**
   * 검색 통계 조회
   */
  static async getSearchStats(
    condition: AuthSearchCondition,
    version: ApiVersion = 'v3'
  ): Promise<SearchStats & { version: ApiVersion; metrics: ApiPerformanceMetrics }> {
    // 버전별 검색 실행
    const { data: result, metrics } = await this.searchByVersion(version, condition);

    return {
      totalRecords: result.totalElements,
      filteredRecords: result.content.length,
      searchDuration: metrics.responseTime,
      fromCache: metrics.responseTime === 0, // 응답시간이 0이면 캐시에서 온 것으로 판단
      version,
      metrics
    };
  }

  /**
   * 최근 로그 조회 (대시보드용)
   */
  static async getRecentLogs(limit = 10, version: ApiVersion = 'v3'): Promise<AuthLog[]> {
    const { data: response } = await this.searchByVersion(version, {
      page: 0,
      size: limit,
      sortBy: 'date',
      direction: 'desc',
    });

    return response.content;
  }

  /**
   * 검색 조건 파라미터 빌드
   */
  private static buildSearchParams(condition: AuthSearchCondition): Record<string, any> {
    const params: Record<string, any> = {};

    // 날짜 범위
    if (condition.startDate) {
      params.startDate = condition.startDate;
    }
    if (condition.endDate) {
      params.endDate = condition.endDate;
    }

    // 필터 조건들
    if (condition.device) {
      params.device = condition.device;
    }
    if (condition.userId) {
      params.userId = condition.userId;
    }
    if (condition.result) {
      params.result = condition.result;
    }
    if (condition.endpoint) {
      params.endpoint = condition.endpoint;
    }

    // 페이지네이션
    if (condition.page !== undefined) {
      params.page = condition.page;
    }
    if (condition.size !== undefined) {
      params.size = condition.size;
    }

    // 정렬
    if (condition.sortBy) {
      params.sortBy = condition.sortBy;
    }
    if (condition.direction) {
      params.direction = condition.direction;
    }

    // V2 커서 기반 페이지네이션
    if (condition.cursorId) {
      params.cursorId = condition.cursorId;
    }
    if (condition.cursorDate) {
      params.cursorDate = condition.cursorDate;
    }

    return params;
  }

  /**
   * 캐시 무효화
   */
  static clearCache(): void {
    ApiCache.clear(`^${this.CACHE_PREFIX}`);
  }

  /**
   * 특정 조건의 캐시 무효화
   */
  static clearCacheForCondition(condition: AuthSearchCondition): void {
    const patterns = [
      `${this.CACHE_PREFIX}-v1-${JSON.stringify(condition)}`,
      `${this.CACHE_PREFIX}-v2-${JSON.stringify(condition)}`,
      `${this.CACHE_PREFIX}-v3-${JSON.stringify(condition)}`,
    ];

    patterns.forEach(pattern => ApiCache.invalidate(pattern));
  }

  /**
   * 로그 내보내기 (CSV 등)
   */
  static async exportLogs(
    condition: AuthSearchCondition,
    format: 'csv' | 'excel' | 'json' = 'csv',
    version: ApiVersion = 'v3'
  ): Promise<Blob> {
    const { data: response } = await this.searchByVersion(version, {
      ...condition,
      page: 0,
      size: 10000, // 대량 조회
    });

    let content: string;
    let mimeType: string;

    switch (format) {
      case 'csv':
        content = this.convertToCSV(response.content);
        mimeType = 'text/csv';
        break;
      case 'json':
        content = JSON.stringify(response.content, null, 2);
        mimeType = 'application/json';
        break;
      case 'excel':
        // Excel 형식은 라이브러리 필요 (xlsx 등)
        content = this.convertToCSV(response.content);
        mimeType = 'text/csv';
        break;
      default:
        throw new Error(`Unsupported format: ${format}`);
    }

    return new Blob([content], { type: mimeType });
  }

  /**
   * CSV 변환
   */
  private static convertToCSV(logs: AuthLog[]): string {
    const headers = ['ID', '날짜', '디바이스', '사용자 ID', '결과', '엔드포인트'];
    const headerRow = headers.join(',');

    const dataRows = logs.map(log => [
      log.id,
      new Date(log.date).toLocaleString('ko-KR'),
      log.device,
      log.userId,
      log.result,
      log.endpoint,
    ].map(field => `"${String(field).replace(/"/g, '""')}"`).join(','));

    return [headerRow, ...dataRows].join('\n');
  }
}