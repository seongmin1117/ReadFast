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
  isLoading: boolean;
  error: string | null;
  searchCondition: AuthSearchCondition;
  stats: SearchStats | null;
  
  // Actions
  search: (condition?: Partial<AuthSearchCondition>) => Promise<void>;
  reset: () => void;
  clearError: () => void;
  clearCache: () => void;
  
  // Pagination helpers
  goToPage: (page: number) => Promise<void>;
  nextPage: () => Promise<void>;
  previousPage: () => Promise<void>;
  
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
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [stats, setStats] = useState<SearchStats | null>(null);
  const [searchCondition, setSearchCondition] = useState<AuthSearchCondition>({
    ...DEFAULT_CONDITION,
    ...initialCondition
  });

  // Computed values
  const logs = data?.content || [];

  // Main search function
  const search = useCallback(async (condition?: Partial<AuthSearchCondition>) => {
    try {
      setIsLoading(true);
      setError(null);

      const newCondition = condition 
        ? { ...searchCondition, ...condition }
        : searchCondition;
      
      setSearchCondition(newCondition);

      const result = await AuthLogService.search(newCondition, useCache);
      setData(result);

      // Update stats from the result
      setStats({
        totalElements: result.totalElements,
        totalPages: result.totalPages, 
        currentPage: result.number,
        pageSize: result.size,
        hasNext: result.hasNext,
        hasPrevious: !result.first
      });

    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '검색 중 오류가 발생했습니다.';
      setError(errorMessage);
      console.error('인증 로그 검색 실패:', err);
    } finally {
      setIsLoading(false);
    }
  }, [searchCondition, useCache]);

  // Reset function
  const reset = useCallback(() => {
    setData(null);
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

  // Pagination helpers
  const goToPage = useCallback(async (page: number) => {
    await search({ page });
  }, [search]);

  const nextPage = useCallback(async () => {
    if (data && !data.last) {
      await goToPage(data.number + 1);
    }
  }, [data, goToPage]);

  const previousPage = useCallback(async () => {
    if (data && !data.first) {
      await goToPage(data.number - 1);
    }
  }, [data, goToPage]);

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
    isLoading,
    error,
    searchCondition,
    stats,
    
    // Actions
    search,
    reset,
    clearError,
    clearCache,
    
    // Pagination helpers
    goToPage,
    nextPage,
    previousPage,
    
    // Data helpers
    getById,
    validateDateRange
  };
}