import { useState, useEffect, useCallback } from 'react';
import { AuthLogService } from '@/services/authLogService';
import type { 
  AuthLog, 
  AuthSearchCondition, 
  PageResponse,
  SearchStats
} from '@/types/auth.types';

interface UseAuthLogsOptions {
  initialCondition?: Partial<AuthSearchCondition>;
  autoSearch?: boolean;
  useCache?: boolean;
}

interface UseAuthLogsReturn {
  data: PageResponse<AuthLog> | null;
  logs: AuthLog[];
  allLogs: AuthLog[]; // 누적된 모든 로그 (더보기 기능용)
  isLoading: boolean;
  isLoadingMore: boolean;
  error: string | null;
  searchCondition: AuthSearchCondition;
  stats: SearchStats | null;
  
  // Actions
  search: (condition?: Partial<AuthSearchCondition>) => Promise<void>;
  loadMore: () => Promise<void>;
  reset: () => void;
  clearError: () => void;
  clearCache: () => void;
  
  // Data helpers
  getById: (id: number) => Promise<AuthLog | null>;
  validateDateRange: (startDate?: Date, endDate?: Date) => boolean;
}

const DEFAULT_CONDITION: AuthSearchCondition = {
  page: 0,
  size: 10,
  sortBy: 'date',
  direction: 'desc'
};

export function useAuthLogs(options: UseAuthLogsOptions = {}): UseAuthLogsReturn {
  const {
    initialCondition = {},
    autoSearch = true,
    useCache = false
  } = options;

  // State
  const [data, setData] = useState<PageResponse<AuthLog> | null>(null);
  const [allLogs, setAllLogs] = useState<AuthLog[]>([]); // 누적된 모든 로그
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [stats, setStats] = useState<SearchStats | null>(null);
  const [searchCondition, setSearchCondition] = useState<AuthSearchCondition>({
    ...DEFAULT_CONDITION,
    ...initialCondition
  });

  // Computed values
  const logs = data?.content || [];

  // Main search function (새로운 검색)
  const search = useCallback(async (condition?: Partial<AuthSearchCondition>) => {
    try {
      setIsLoading(true);
      setError(null);

      const newCondition = condition 
        ? { ...searchCondition, ...condition, cursorId: undefined, cursorDate: undefined } // 새 검색이므로 커서 초기화
        : { ...searchCondition, cursorId: undefined, cursorDate: undefined };
      
      setSearchCondition(newCondition);

      const result = await AuthLogService.search(newCondition, useCache);
      setData(result);
      setAllLogs(result.content); // 새로운 검색이므로 allLogs 초기화

      // Update stats from the result
      setStats({
        currentResultCount: result.content.length,
        pageSize: result.size,
        hasNext: result.hasNext,
        isCursorBased: result.isCursorBased
      });

    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '검색 중 오류가 발생했습니다.';
      setError(errorMessage);
      console.error('인증 로그 검색 실패:', err);
    } finally {
      setIsLoading(false);
    }
  }, [searchCondition, useCache]);

  // Load more function (더보기)
  const loadMore = useCallback(async () => {
    if (!data || !data.hasNext || isLoadingMore) return;

    try {
      setIsLoadingMore(true);
      setError(null);

      // 마지막 항목의 커서 정보 사용
      const lastItem = allLogs[allLogs.length - 1];
      if (!lastItem) return;

      const moreCondition = {
        ...searchCondition,
        cursorId: lastItem.id,
        cursorDate: new Date(lastItem.date),
        page: 0 // 커서 기반에서는 항상 0
      };

      const result = await AuthLogService.search(moreCondition, useCache);
      
      // 기존 데이터에 새 데이터 추가
      const newAllLogs = [...allLogs, ...result.content];
      setAllLogs(newAllLogs);
      setData(result); // 최신 페이지 정보로 업데이트

      // Update stats
      setStats({
        currentResultCount: newAllLogs.length,
        pageSize: result.size,
        hasNext: result.hasNext,
        isCursorBased: result.isCursorBased
      });

    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '더보기 중 오류가 발생했습니다.';
      setError(errorMessage);
      console.error('더보기 실패:', err);
    } finally {
      setIsLoadingMore(false);
    }
  }, [data, allLogs, searchCondition, useCache, isLoadingMore]);

  // Reset function
  const reset = useCallback(() => {
    setData(null);
    setAllLogs([]);
    setError(null);
    setStats(null);
    setSearchCondition(DEFAULT_CONDITION);
  }, []);

  // Clear error
  const clearError = useCallback(() => {
    setError(null);
  }, []);

  // Clear cache
  const clearCache = useCallback(() => {
    AuthLogService.clearCache();
  }, []);

  // Data helpers
  const getById = useCallback(async (id: number): Promise<AuthLog | null> => {
    try {
      return await AuthLogService.getById(id);
    } catch (err) {
      console.error(`로그 조회 실패 (ID: ${id}):`, err);
      return null;
    }
  }, []);

  const validateDateRange = useCallback((startDate?: Date, endDate?: Date): boolean => {
    return AuthLogService.validateDateRange(startDate, endDate);
  }, []);

  // Auto search on mount
  useEffect(() => {
    if (autoSearch) {
      search();
    }
  }, []); // 의도적으로 빈 배열로 설정 (마운트 시 한 번만 실행)

  return {
    data,
    logs,
    allLogs,
    isLoading,
    isLoadingMore,
    error,
    searchCondition,
    stats,
    
    // Actions
    search,
    loadMore,
    reset,
    clearError,
    clearCache,
    
    // Data helpers
    getById,
    validateDateRange
  };
}