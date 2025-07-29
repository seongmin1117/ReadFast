import { useState, useEffect, useCallback } from 'react';
import { DashboardService } from '@/services/dashboardService';

interface DashboardStats {
  totalLogs: number;
  dailyLogs: number;
  successRate: number;
  activeUsers: number;
  alertsToday: number;
}

interface SystemHealth {
  isHealthy: boolean;
  issues: string[];
  warnings: string[];
}

interface RealtimeStats {
  timestamp: string;
  activeConnections: number;
  requestsPerMinute: number;
  averageResponseTime: number;
  errorRate: number;
}

interface UseDashboardReturn {
  // 통계 데이터
  stats: DashboardStats | null;
  systemHealth: SystemHealth | null;
  realtimeStats: RealtimeStats | null;
  
  // 로딩 상태
  isLoadingStats: boolean;
  isLoadingHealth: boolean;
  isLoadingRealtime: boolean;
  
  // 에러 상태
  statsError: string | null;
  healthError: string | null;
  realtimeError: string | null;
  
  // 액션
  refreshStats: () => Promise<void>;
  refreshHealth: () => Promise<void>;
  refreshRealtime: () => Promise<void>;
  refreshAll: () => Promise<void>;
}

export function useDashboard(): UseDashboardReturn {
  // 통계 상태
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [isLoadingStats, setIsLoadingStats] = useState(false);
  const [statsError, setStatsError] = useState<string | null>(null);

  // 시스템 헬스 상태
  const [systemHealth, setSystemHealth] = useState<SystemHealth | null>(null);
  const [isLoadingHealth, setIsLoadingHealth] = useState(false);
  const [healthError, setHealthError] = useState<string | null>(null);

  // 실시간 통계 상태
  const [realtimeStats, setRealtimeStats] = useState<RealtimeStats | null>(null);
  const [isLoadingRealtime, setIsLoadingRealtime] = useState(false);
  const [realtimeError, setRealtimeError] = useState<string | null>(null);

  // 대시보드 통계 새로고침
  const refreshStats = useCallback(async () => {
    try {
      setIsLoadingStats(true);
      setStatsError(null);
      
      const data = await DashboardService.getDashboardStats();
      setStats(data);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '통계 조회 중 오류가 발생했습니다.';
      setStatsError(errorMessage);
      console.error('Failed to fetch dashboard stats:', err);
    } finally {
      setIsLoadingStats(false);
    }
  }, []);

  // 시스템 헬스 새로고침
  const refreshHealth = useCallback(async () => {
    try {
      setIsLoadingHealth(true);
      setHealthError(null);
      
      const data = await DashboardService.getSystemHealth();
      setSystemHealth(data);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '시스템 상태 조회 중 오류가 발생했습니다.';
      setHealthError(errorMessage);
      console.error('Failed to fetch system health:', err);
    } finally {
      setIsLoadingHealth(false);
    }
  }, []);

  // 실시간 통계 새로고침
  const refreshRealtime = useCallback(async () => {
    try {
      setIsLoadingRealtime(true);
      setRealtimeError(null);
      
      const data = await DashboardService.getRealtimeStats();
      setRealtimeStats(data);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '실시간 통계 조회 중 오류가 발생했습니다.';
      setRealtimeError(errorMessage);
      console.error('Failed to fetch realtime stats:', err);
    } finally {
      setIsLoadingRealtime(false);
    }
  }, []);

  // 전체 새로고침
  const refreshAll = useCallback(async () => {
    await Promise.all([
      refreshStats(),
      refreshHealth(),
      refreshRealtime(),
    ]);
  }, [refreshStats, refreshHealth, refreshRealtime]);

  // 초기 로드
  useEffect(() => {
    refreshAll();
  }, [refreshAll]);

  // 주기적 업데이트
  useEffect(() => {
    // 통계는 5분마다 업데이트
    const statsInterval = setInterval(refreshStats, 5 * 60 * 1000);
    
    // 시스템 헬스는 1분마다 업데이트
    const healthInterval = setInterval(refreshHealth, 60 * 1000);
    
    // 실시간 통계는 30초마다 업데이트
    const realtimeInterval = setInterval(refreshRealtime, 30 * 1000);

    return () => {
      clearInterval(statsInterval);
      clearInterval(healthInterval);
      clearInterval(realtimeInterval);
    };
  }, [refreshStats, refreshHealth, refreshRealtime]);

  return {
    // 데이터
    stats,
    systemHealth,
    realtimeStats,
    
    // 로딩 상태
    isLoadingStats,
    isLoadingHealth,
    isLoadingRealtime,
    
    // 에러 상태
    statsError,
    healthError,
    realtimeError,
    
    // 액션
    refreshStats,
    refreshHealth,
    refreshRealtime,
    refreshAll,
  };
}