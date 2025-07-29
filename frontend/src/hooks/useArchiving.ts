import { useState, useEffect, useCallback } from 'react';
import { ArchivingService } from '@/services/archivingService';
import type { ArchivingResult } from '@/types/auth.types';
import type { 
  ArchiveMetadata,
  PolicyExecutionResult
} from '@/types/policy.types';

interface UseArchivingReturn {
  // Status
  status: {
    isRunning: boolean;
    lastExecution?: PolicyExecutionResult;
    nextScheduledTime?: string;
    queueSize: number;
  } | null;
  isLoadingStatus: boolean;
  statusError: string | null;
  
  // Actions
  executeArchiving: () => Promise<ArchivingResult | null>;
  executeArchivingByDate: (targetDate: string) => Promise<ArchivingResult | null>;
  pauseArchiving: () => Promise<void>;
  resumeArchiving: () => Promise<void>;
  refreshStatus: () => Promise<void>;
  
  // Execution state
  isExecuting: boolean;
  executionError: string | null;
  lastExecutionResult: ArchivingResult | null;
}

export function useArchiving(): UseArchivingReturn {
  const [status, setStatus] = useState<UseArchivingReturn['status']>(null);
  const [isLoadingStatus, setIsLoadingStatus] = useState(false);
  const [statusError, setStatusError] = useState<string | null>(null);
  
  const [isExecuting, setIsExecuting] = useState(false);
  const [executionError, setExecutionError] = useState<string | null>(null);
  const [lastExecutionResult, setLastExecutionResult] = useState<ArchivingResult | null>(null);

  // 상태 조회
  const refreshStatus = useCallback(async () => {
    try {
      setIsLoadingStatus(true);
      setStatusError(null);
      
      const result = await ArchivingService.getArchivingStatus();
      setStatus(result);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '상태 조회 중 오류가 발생했습니다.';
      setStatusError(errorMessage);
      console.error('Failed to fetch archiving status:', err);
    } finally {
      setIsLoadingStatus(false);
    }
  }, []);

  // 아카이빙 실행
  const executeArchiving = useCallback(async (): Promise<ArchivingResult | null> => {
    try {
      setIsExecuting(true);
      setExecutionError(null);
      
      const result = await ArchivingService.executeArchivingBatch();
      setLastExecutionResult(result);
      
      // 실행 후 상태 새로고침
      setTimeout(refreshStatus, 1000);
      
      return result;
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '아카이빙 실행 중 오류가 발생했습니다.';
      setExecutionError(errorMessage);
      console.error('Failed to execute archiving:', err);
      return null;
    } finally {
      setIsExecuting(false);
    }
  }, [refreshStatus]);

  // 날짜 지정 아카이빙 실행
  const executeArchivingByDate = useCallback(async (targetDate: string): Promise<ArchivingResult | null> => {
    try {
      setIsExecuting(true);
      setExecutionError(null);
      
      const result = await ArchivingService.executeArchivingBatchByDate(targetDate);
      setLastExecutionResult(result);
      
      // 실행 후 상태 새로고침
      setTimeout(refreshStatus, 1000);
      
      return result;
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '날짜 지정 아카이빙 실행 중 오류가 발생했습니다.';
      setExecutionError(errorMessage);
      console.error('Failed to execute archiving by date:', err);
      return null;
    } finally {
      setIsExecuting(false);
    }
  }, [refreshStatus]);

  // 아카이빙 일시 중지
  const pauseArchiving = useCallback(async () => {
    try {
      await ArchivingService.pauseArchiving();
      await refreshStatus();
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '아카이빙 일시 중지 중 오류가 발생했습니다.';
      setStatusError(errorMessage);
      console.error('Failed to pause archiving:', err);
    }
  }, [refreshStatus]);

  // 아카이빙 재개
  const resumeArchiving = useCallback(async () => {
    try {
      await ArchivingService.resumeArchiving();
      await refreshStatus();
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '아카이빙 재개 중 오류가 발생했습니다.';
      setStatusError(errorMessage);
      console.error('Failed to resume archiving:', err);
    }
  }, [refreshStatus]);

  // 초기 상태 로드 및 주기적 갱신
  useEffect(() => {
    refreshStatus();
    
    // 30초마다 상태 갱신
    const interval = setInterval(refreshStatus, 30000);
    
    return () => clearInterval(interval);
  }, [refreshStatus]);

  return {
    // Status
    status,
    isLoadingStatus,
    statusError,
    
    // Actions
    executeArchiving,
    executeArchivingByDate,
    pauseArchiving,
    resumeArchiving,
    refreshStatus,
    
    // Execution state
    isExecuting,
    executionError,
    lastExecutionResult,
  };
}

// 아카이브 메타데이터 관리 훅
interface UseArchiveMetadataOptions {
  initialPageSize?: number;
  autoLoad?: boolean;
}

interface UseArchiveMetadataReturn {
  metadata: ArchiveMetadata[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  hasNext: boolean;
  isLoading: boolean;
  error: string | null;
  
  // Actions
  loadMetadata: (params?: any) => Promise<void>;
  goToPage: (page: number) => Promise<void>;
  changePageSize: (size: number) => Promise<void>;
  downloadArchive: (archiveId: number) => Promise<void>;
  verifyArchive: (archiveId: number) => Promise<any>;
  deleteArchive: (archiveId: number) => Promise<void>;
  refresh: () => Promise<void>;
}

export function useArchiveMetadata(options: UseArchiveMetadataOptions = {}): UseArchiveMetadataReturn {
  const { initialPageSize = 20, autoLoad = true } = options;
  
  const [metadata, setMetadata] = useState<ArchiveMetadata[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pageSize, setPageSize] = useState(initialPageSize);

  // 메타데이터 로드
  const loadMetadata = useCallback(async (params = {}) => {
    try {
      setIsLoading(true);
      setError(null);
      
      const result = await ArchivingService.getArchiveMetadataList({
        page: currentPage,
        size: pageSize,
        ...params,
      });
      
      setMetadata(result.content);
      setTotalElements(result.totalElements);
      setTotalPages(result.totalPages);
      setHasNext(result.hasNext);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '메타데이터 조회 중 오류가 발생했습니다.';
      setError(errorMessage);
      console.error('Failed to load archive metadata:', err);
    } finally {
      setIsLoading(false);
    }
  }, [currentPage, pageSize]);

  // 페이지 이동
  const goToPage = useCallback(async (page: number) => {
    setCurrentPage(page);
  }, []);

  // 페이지 크기 변경
  const changePageSize = useCallback(async (size: number) => {
    setPageSize(size);
    setCurrentPage(0);
  }, []);

  // 아카이브 다운로드
  const downloadArchive = useCallback(async (archiveId: number) => {
    try {
      const blob = await ArchivingService.downloadArchiveFile(archiveId);
      
      // 파일명 생성
      const archive = metadata.find(m => m.id === archiveId);
      const filename = archive?.fileName || `archive-${archiveId}.zip`;
      
      // 다운로드 실행
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '다운로드 중 오류가 발생했습니다.';
      setError(errorMessage);
      console.error('Failed to download archive:', err);
    }
  }, [metadata]);

  // 아카이브 검증
  const verifyArchive = useCallback(async (archiveId: number) => {
    try {
      const result = await ArchivingService.verifyArchiveFile(archiveId);
      
      // 메타데이터 새로고침
      await loadMetadata();
      
      return result;
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '검증 중 오류가 발생했습니다.';
      setError(errorMessage);
      console.error('Failed to verify archive:', err);
      throw err;
    }
  }, [loadMetadata]);

  // 아카이브 삭제
  const deleteArchive = useCallback(async (archiveId: number) => {
    try {
      await ArchivingService.deleteArchiveFile(archiveId);
      
      // 메타데이터 새로고침
      await loadMetadata();
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '삭제 중 오류가 발생했습니다.';
      setError(errorMessage);
      console.error('Failed to delete archive:', err);
    }
  }, [loadMetadata]);

  // 새로고침
  const refresh = useCallback(async () => {
    await loadMetadata();
  }, [loadMetadata]);

  // 페이지나 페이지 크기 변경 시 자동 로드
  useEffect(() => {
    if (autoLoad) {
      loadMetadata();
    }
  }, [currentPage, pageSize, loadMetadata, autoLoad]);

  return {
    metadata,
    totalElements,
    totalPages,
    currentPage,
    hasNext,
    isLoading,
    error,
    
    // Actions
    loadMetadata,
    goToPage,
    changePageSize,
    downloadArchive,
    verifyArchive,
    deleteArchive,
    refresh,
  };
}

// 아카이빙 통계 훅
interface UseArchivingStatsReturn {
  stats: {
    totalArchivedRecords: number;
    totalArchiveFiles: number;
    totalArchivedSize: number;
    compressionRatio: number;
    recentExecutions: number;
    failureRate: number;
    averageExecutionTime: number;
    oldestArchive?: string;
    newestArchive?: string;
  } | null;
  storageUsage: {
    totalSize: number;
    usedSize: number;
    availableSize: number;
    usagePercentage: number;
    fileCount: number;
    storageType: string;
    lastUpdated: string;
  } | null;
  isLoading: boolean;
  error: string | null;
  
  refresh: () => Promise<void>;
}

export function useArchivingStats(): UseArchivingStatsReturn {
  const [stats, setStats] = useState<UseArchivingStatsReturn['stats']>(null);
  const [storageUsage, setStorageUsage] = useState<UseArchivingStatsReturn['storageUsage']>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      
      // 병렬로 통계 조회
      const [statsResult, storageResult] = await Promise.all([
        ArchivingService.getArchivingStats(),
        ArchivingService.getStorageUsage(),
      ]);
      
      setStats(statsResult);
      setStorageUsage(storageResult);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '통계 조회 중 오류가 발생했습니다.';
      setError(errorMessage);
      console.error('Failed to fetch archiving stats:', err);
    } finally {
      setIsLoading(false);
    }
  }, []);

  // 초기 로드 및 주기적 갱신
  useEffect(() => {
    refresh();
    
    // 5분마다 통계 갱신
    const interval = setInterval(refresh, 300000);
    
    return () => clearInterval(interval);
  }, [refresh]);

  return {
    stats,
    storageUsage,
    isLoading,
    error,
    refresh,
  };
}