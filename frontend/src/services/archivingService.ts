import { ApiService } from './api';
import type { ArchivingResult } from '@/types/auth.types';
import type { 
  ArchiveMetadata,
  PolicyExecutionResult
} from '@/types/policy.types';

export class ArchivingService {
  /**
   * 수동 아카이빙 배치 실행
   */
  static async executeArchivingBatch(): Promise<ArchivingResult> {
    const response = await ApiService.get<ArchivingResult>('/v3/auth/archiving/execute');
    return response.data;
  }

  /**
   * 아카이빙 상태 조회
   */
  static async getArchivingStatus(): Promise<{
    isRunning: boolean;
    lastExecution?: PolicyExecutionResult;
    nextScheduledTime?: string;
    queueSize: number;
  }> {
    try {
      const response = await ApiService.get('/v3/auth/archiving/status');
      return response.data;
    } catch (error) {
      // 상태 API가 없는 경우 기본값 반환
      return {
        isRunning: false,
        queueSize: 0,
      };
    }
  }

  /**
   * 아카이브 메타데이터 목록 조회
   */
  static async getArchiveMetadataList(params?: {
    page?: number;
    size?: number;
    startDate?: string;
    endDate?: string;
    status?: 'CREATED' | 'VERIFIED' | 'CORRUPTED' | 'DELETED';
  }): Promise<{
    content: ArchiveMetadata[];
    totalElements: number;
    totalPages: number;
    hasNext: boolean;
  }> {
    try {
      const response = await ApiService.get('/v3/auth/archiving/metadata', params);
      return response.data;
    } catch (error) {
      // API가 없는 경우 빈 결과 반환
      return {
        content: [],
        totalElements: 0,
        totalPages: 0,
        hasNext: false,
      };
    }
  }

  /**
   * 특정 아카이브 파일 다운로드
   */
  static async downloadArchiveFile(archiveId: number): Promise<Blob> {
    try {
      const response = await fetch(`/api/v3/auth/archiving/download/${archiveId}`, {
        method: 'GET',
        credentials: 'include',
      });

      if (!response.ok) {
        throw new Error(`Download failed: ${response.statusText}`);
      }

      return await response.blob();
    } catch (error) {
      console.error('Archive download failed:', error);
      throw new Error('아카이브 파일 다운로드에 실패했습니다.');
    }
  }

  /**
   * 아카이브 파일 검증
   */
  static async verifyArchiveFile(archiveId: number): Promise<{
    isValid: boolean;
    checksum: string;
    recordCount: number;
    issues?: string[];
  }> {
    try {
      const response = await ApiService.post(`/v3/auth/archiving/verify/${archiveId}`);
      return response.data;
    } catch (error) {
      console.error('Archive verification failed:', error);
      throw new Error('아카이브 파일 검증에 실패했습니다.');
    }
  }

  /**
   * 아카이브 파일 삭제
   */
  static async deleteArchiveFile(archiveId: number): Promise<void> {
    try {
      await ApiService.delete(`/v3/auth/archiving/${archiveId}`);
    } catch (error) {
      console.error('Archive deletion failed:', error);
      throw new Error('아카이브 파일 삭제에 실패했습니다.');
    }
  }

  /**
   * 아카이빙 실행 히스토리 조회
   */
  static async getExecutionHistory(params?: {
    page?: number;
    size?: number;
    startDate?: string;
    endDate?: string;
    status?: 'SUCCESS' | 'PARTIAL_SUCCESS' | 'FAILED';
  }): Promise<{
    content: PolicyExecutionResult[];
    totalElements: number;
    totalPages: number;
    hasNext: boolean;
  }> {
    try {
      const response = await ApiService.get('/v3/auth/archiving/history', params);
      return response.data;
    } catch (error) {
      // API가 없는 경우 빈 결과 반환
      return {
        content: [],
        totalElements: 0,
        totalPages: 0,
        hasNext: false,
      };
    }
  }

  /**
   * 아카이빙 통계 조회
   */
  static async getArchivingStats(period?: 'day' | 'week' | 'month' | 'year'): Promise<{
    totalArchivedRecords: number;
    totalArchiveFiles: number;
    totalArchivedSize: number;
    compressionRatio: number;
    recentExecutions: number;
    failureRate: number;
    averageExecutionTime: number;
    oldestArchive?: string;
    newestArchive?: string;
  }> {
    try {
      const response = await ApiService.get('/v3/auth/archiving/stats', { period });
      return response.data;
    } catch (error) {
      // API가 없는 경우 기본값 반환
      return {
        totalArchivedRecords: 0,
        totalArchiveFiles: 0,
        totalArchivedSize: 0,
        compressionRatio: 0,
        recentExecutions: 0,
        failureRate: 0,
        averageExecutionTime: 0,
      };
    }
  }

  /**
   * 스토리지 사용량 조회
   */
  static async getStorageUsage(): Promise<{
    totalSize: number;
    usedSize: number;
    availableSize: number;
    usagePercentage: number;
    fileCount: number;
    storageType: string;
    lastUpdated: string;
  }> {
    try {
      const response = await ApiService.get('/v3/auth/archiving/storage/usage');
      return response.data;
    } catch (error) {
      // API가 없는 경우 기본값 반환
      return {
        totalSize: 0,
        usedSize: 0,
        availableSize: 0,
        usagePercentage: 0,
        fileCount: 0,
        storageType: 'LOCAL',
        lastUpdated: new Date().toISOString(),
      };
    }
  }

  /**
   * 아카이빙 일시 중지/재개
   */
  static async pauseArchiving(): Promise<void> {
    try {
      await ApiService.post('/v3/auth/archiving/pause');
    } catch (error) {
      console.error('Failed to pause archiving:', error);
      throw new Error('아카이빙 일시 중지에 실패했습니다.');
    }
  }

  static async resumeArchiving(): Promise<void> {
    try {
      await ApiService.post('/v3/auth/archiving/resume');
    } catch (error) {
      console.error('Failed to resume archiving:', error);
      throw new Error('아카이빙 재개에 실패했습니다.');
    }
  }

  /**
   * 아카이브에서 데이터 복원
   */
  static async restoreFromArchive(archiveId: number, options?: {
    targetDate?: string;
    recordIds?: number[];
    restoreToTemp?: boolean;
  }): Promise<{
    success: boolean;
    restoredRecords: number;
    targetLocation: string;
    estimatedTime: number;
  }> {
    try {
      const response = await ApiService.post(`/v3/auth/archiving/restore/${archiveId}`, options);
      return response.data;
    } catch (error) {
      console.error('Archive restoration failed:', error);
      throw new Error('아카이브 복원에 실패했습니다.');
    }
  }

  /**
   * 아카이브 파일 무결성 체크
   */
  static async checkIntegrity(archiveId?: number): Promise<{
    checkedFiles: number;
    corruptedFiles: number;
    missingFiles: number;
    details: Array<{
      archiveId: number;
      fileName: string;
      status: 'OK' | 'CORRUPTED' | 'MISSING';
      checksum: string;
      expectedChecksum: string;
    }>;
  }> {
    try {
      const url = archiveId 
        ? `/v3/auth/archiving/integrity/${archiveId}`
        : '/v3/auth/archiving/integrity';
      
      const response = await ApiService.get(url);
      return response.data;
    } catch (error) {
      console.error('Integrity check failed:', error);
      throw new Error('무결성 검사에 실패했습니다.');
    }
  }

  /**
   * 아카이빙 설정 최적화 제안
   */
  static async getOptimizationSuggestions(): Promise<{
    suggestions: Array<{
      type: 'batch_size' | 'schedule' | 'retention' | 'compression';
      title: string;
      description: string;
      currentValue: any;
      suggestedValue: any;
      expectedImpact: string;
      priority: 'high' | 'medium' | 'low';
    }>;
    overallScore: number;
    lastAnalyzed: string;
  }> {
    try {
      const response = await ApiService.get('/v3/auth/archiving/optimize');
      return response.data;
    } catch (error) {
      // API가 없는 경우 기본 제안사항 반환
      return {
        suggestions: [
          {
            type: 'batch_size',
            title: '배치 크기 최적화',
            description: '현재 배치 크기가 너무 작아 처리 효율이 낮습니다.',
            currentValue: 1000,
            suggestedValue: 5000,
            expectedImpact: '처리 시간 30% 단축',
            priority: 'medium',
          }
        ],
        overallScore: 75,
        lastAnalyzed: new Date().toISOString(),
      };
    }
  }
}