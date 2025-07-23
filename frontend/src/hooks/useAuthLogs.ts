import { useState, useEffect, useCallback } from 'react';
import { AuthLogService } from '@/services/authLogService';
import type { 
  AuthLog, 
  AuthSearchCondition, 
  PageResponse,
  SearchStats,
  ApiVersion,
  ApiPerformanceMetrics
} from '@/types/auth.types';

interface UseAuthLogsOptions {
  initialCondition?: Partial<AuthSearchCondition>;
  autoSearch?: boolean;
  useCache?: boolean;
  defaultVersion?: ApiVersion;
}

interface UseAuthLogsReturn {
  data: PageResponse<AuthLog> | null;
  logs: AuthLog[];
  isLoading: boolean;
  error: string | null;
  searchCondition: AuthSearchCondition;
  stats: SearchStats | null;
  
  // Version management
  currentVersion: ApiVersion;
  setVersion: (version: ApiVersion) => void;
  performanceMetrics: ApiPerformanceMetrics | null;
  allPerformanceMetrics: ApiPerformanceMetrics[];
  
  // Actions
  search: (condition?: Partial<AuthSearchCondition>, version?: ApiVersion) => Promise<void>;
  searchAllVersions: (condition?: Partial<AuthSearchCondition>) => Promise<void>;
  refresh: () => Promise<void>;
  clearData: () => void;
  updateCondition: (updates: Partial<AuthSearchCondition>) => void;
  
  // Pagination
  goToPage: (page: number) => Promise<void>;
  changePageSize: (size: number) => Promise<void>;
  
  // Export
  exportLogs: (format?: 'csv' | 'excel' | 'json', version?: ApiVersion) => Promise<void>;
  
  // Performance
  clearPerformanceMetrics: () => void;
}

const defaultCondition: AuthSearchCondition = {
  page: 0,
  size: 20,
  sortBy: 'date',
  direction: 'desc',
};

export function useAuthLogs(options: UseAuthLogsOptions = {}): UseAuthLogsReturn {
  const {
    initialCondition = {},
    autoSearch = true,
    useCache = false,
    defaultVersion = 'v3'
  } = options;

  const [data, setData] = useState<PageResponse<AuthLog> | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [stats, setStats] = useState<SearchStats | null>(null);
  const [currentVersion, setCurrentVersion] = useState<ApiVersion>(defaultVersion);
  const [performanceMetrics, setPerformanceMetrics] = useState<ApiPerformanceMetrics | null>(null);
  const [searchCondition, setSearchCondition] = useState<AuthSearchCondition>({
    ...defaultCondition,
    ...initialCondition,
  });

  // 모든 성능 메트릭 조회
  const getAllPerformanceMetrics = useCallback((): ApiPerformanceMetrics[] => {
    return AuthLogService.getPerformanceMetrics();
  }, []);

  // 버전 설정
  const setVersion = useCallback((version: ApiVersion) => {
    setCurrentVersion(version);
  }, []);

  // 검색 실행
  const search = useCallback(async (condition?: Partial<AuthSearchCondition>, version?: ApiVersion) => {
    try {
      setIsLoading(true);
      setError(null);

      const searchVersion = version || currentVersion;
      const finalCondition = condition 
        ? { ...searchCondition, ...condition }
        : searchCondition;

      // 조건 업데이트
      setSearchCondition(finalCondition);

      // 버전별 검색 실행
      const { data: result, metrics } = await AuthLogService.searchByVersion(
        searchVersion, 
        finalCondition, 
        useCache
      );
      
      setData(result);
      setPerformanceMetrics(metrics);

      // 통계 조회
      try {
        const statsResult = await AuthLogService.getSearchStats(finalCondition, searchVersion);
        setStats(statsResult);
      } catch (statsError) {
        console.warn('Failed to fetch search stats:', statsError);
      }

    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '검색 중 오류가 발생했습니다.';
      setError(errorMessage);
      console.error('Search failed:', err);
    } finally {
      setIsLoading(false);
    }
  }, [searchCondition, currentVersion, useCache]);

  // 모든 버전에서 검색 (성능 비교용)
  const searchAllVersions = useCallback(async (condition?: Partial<AuthSearchCondition>) => {
    try {
      setIsLoading(true);
      setError(null);

      const finalCondition = condition 
        ? { ...searchCondition, ...condition }
        : searchCondition;

      // 조건 업데이트
      setSearchCondition(finalCondition);

      // 모든 버전에서 병렬로 검색 실행
      const versions: ApiVersion[] = ['v1', 'v2', 'v3'];
      const promises = versions.map(v => 
        AuthLogService.searchByVersion(v, finalCondition, useCache)
          .catch(error => ({ error, version: v }))
      );

      const results = await Promise.all(promises);
      
      // 가장 최근 버전의 결과를 메인 데이터로 설정
      const v3Result = results.find((r, i) => versions[i] === 'v3' && !('error' in r));
      if (v3Result && !('error' in v3Result)) {
        setData(v3Result.data);
        setPerformanceMetrics(v3Result.metrics);
      }

      console.log('Performance comparison results:', results);

    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '검색 중 오류가 발생했습니다.';
      setError(errorMessage);
      console.error('Search all versions failed:', err);
    } finally {
      setIsLoading(false);
    }
  }, [searchCondition, useCache]);

  // 새로고침
  const refresh = useCallback(async () => {
    await search();
  }, [search]);

  // 데이터 초기화
  const clearData = useCallback(() => {
    setData(null);
    setError(null);
    setStats(null);
  }, []);

  // 검색 조건 업데이트
  const updateCondition = useCallback((updates: Partial<AuthSearchCondition>) => {
    setSearchCondition(prev => ({ ...prev, ...updates }));
  }, []);

  // 페이지 이동
  const goToPage = useCallback(async (page: number) => {
    await search({ page });
  }, [search]);

  // 페이지 크기 변경
  const changePageSize = useCallback(async (size: number) => {
    await search({ size, page: 0 }); // 페이지 크기 변경 시 첫 페이지로
  }, [search]);

  // 로그 내보내기
  const exportLogs = useCallback(async (format: 'csv' | 'excel' | 'json' = 'csv', version?: ApiVersion) => {
    try {
      setIsLoading(true);
      
      const exportVersion = version || currentVersion;
      const blob = await AuthLogService.exportLogs(searchCondition, format, exportVersion);
      
      // 파일 다운로드
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `auth-logs-${exportVersion}-${new Date().toISOString().split('T')[0]}.${format}`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);

    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '내보내기 중 오류가 발생했습니다.';
      setError(errorMessage);
      console.error('Export failed:', err);
    } finally {
      setIsLoading(false);
    }
  }, [searchCondition, currentVersion]);

  // 성능 메트릭 초기화
  const clearPerformanceMetrics = useCallback(() => {
    AuthLogService.clearPerformanceMetrics();
    setPerformanceMetrics(null);
  }, []);

  // 초기 검색 실행
  useEffect(() => {
    if (autoSearch) {
      search();
    }
  }, []); // 의존성 배열을 비워서 마운트 시에만 실행

  return {
    data,
    logs: data?.content || [],
    isLoading,
    error,
    searchCondition,
    stats,
    
    // Version management
    currentVersion,
    setVersion,
    performanceMetrics,
    allPerformanceMetrics: getAllPerformanceMetrics(),
    
    // Actions
    search,
    searchAllVersions,
    refresh,
    clearData,
    updateCondition,
    
    // Pagination
    goToPage,
    changePageSize,
    
    // Export
    exportLogs,
    
    // Performance
    clearPerformanceMetrics,
  };
}

// 최근 로그 조회용 훅
export function useRecentAuthLogs(limit = 10, version: ApiVersion = 'v3') {
  const [logs, setLogs] = useState<AuthLog[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchRecentLogs = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      
      const result = await AuthLogService.getRecentLogs(limit, version);
      setLogs(result);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '최근 로그 조회 중 오류가 발생했습니다.';
      setError(errorMessage);
      console.error('Failed to fetch recent logs:', err);
    } finally {
      setIsLoading(false);
    }
  }, [limit, version]);

  useEffect(() => {
    fetchRecentLogs();
  }, [fetchRecentLogs]);

  return {
    logs,
    isLoading,
    error,
    refresh: fetchRecentLogs,
  };
}

// 단일 로그 조회용 훅
export function useAuthLog(id: number | null) {
  const [log, setLog] = useState<AuthLog | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchLog = useCallback(async (logId: number) => {
    try {
      setIsLoading(true);
      setError(null);
      
      const result = await AuthLogService.getById(logId);
      setLog(result);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '로그 조회 중 오류가 발생했습니다.';
      setError(errorMessage);
      console.error('Failed to fetch auth log:', err);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (id) {
      fetchLog(id);
    } else {
      setLog(null);
      setError(null);
    }
  }, [id, fetchLog]);

  return {
    log,
    isLoading,
    error,
    refresh: id ? () => fetchLog(id) : undefined,
  };
}