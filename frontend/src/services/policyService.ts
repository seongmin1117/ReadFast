import { ApiService } from './api';
import type { 
  DataRetentionPolicy,
  UpdatePolicyRequest,
  PolicyExecutionResult
} from '@/types/policy.types';

export class PolicyService {
  /**
   * 정책 조회
   */
  static async getPolicy(policyId: number): Promise<DataRetentionPolicy> {
    const response = await ApiService.get<DataRetentionPolicy>(`/v1/policies/${policyId}`);
    return response.data;
  }

  /**
   * 기본 정책 조회 (ID = 1로 가정)
   */
  static async getDefaultPolicy(): Promise<DataRetentionPolicy> {
    return this.getPolicy(1);
  }

  /**
   * 정책 업데이트
   */
  static async updatePolicy(
    policyId: number, 
    request: UpdatePolicyRequest
  ): Promise<DataRetentionPolicy> {
    const response = await ApiService.put<DataRetentionPolicy>(
      `/v1/policies/${policyId}`,
      request
    );
    return response.data;
  }

  /**
   * 기본 정책 업데이트
   */
  static async updateDefaultPolicy(request: UpdatePolicyRequest): Promise<DataRetentionPolicy> {
    return this.updatePolicy(1, request);
  }

  /**
   * 모든 정책 목록 조회 (향후 확장용)
   */
  static async getAllPolicies(): Promise<DataRetentionPolicy[]> {
    try {
      const response = await ApiService.get<DataRetentionPolicy[]>('/v1/policies');
      return response.data;
    } catch (error) {
      // 단일 정책만 지원하는 경우 기본 정책 반환
      const defaultPolicy = await this.getDefaultPolicy();
      return [defaultPolicy];
    }
  }

  /**
   * 정책 생성 (향후 확장용)
   */
  static async createPolicy(policy: Omit<DataRetentionPolicy, 'id' | 'createdAt' | 'updatedAt'>): Promise<DataRetentionPolicy> {
    const response = await ApiService.post<DataRetentionPolicy>('/v1/policies', policy);
    return response.data;
  }

  /**
   * 정책 삭제 (향후 확장용)
   */
  static async deletePolicy(policyId: number): Promise<void> {
    await ApiService.delete(`/v1/policies/${policyId}`);
  }

  /**
   * 정책 복사 (향후 확장용)
   */
  static async clonePolicy(policyId: number, newName: string): Promise<DataRetentionPolicy> {
    const originalPolicy = await this.getPolicy(policyId);
    
    const clonedPolicy = {
      ...originalPolicy,
      name: newName,
    };

    // id, createdAt, updatedAt 제거
    const { id, createdAt, updatedAt, ...policyData } = clonedPolicy;
    
    return this.createPolicy(policyData);
  }

  /**
   * 정책 검증
   */
  static async validatePolicy(policy: UpdatePolicyRequest): Promise<{
    isValid: boolean;
    errors: string[];
    warnings: string[];
  }> {
    try {
      const response = await ApiService.post('/v1/policies/validate', policy);
      return response.data;
    } catch (error) {
      // 클라이언트 측 검증 로직
      return this.clientSideValidation(policy);
    }
  }

  /**
   * 클라이언트 측 정책 검증
   */
  private static clientSideValidation(policy: UpdatePolicyRequest): {
    isValid: boolean;
    errors: string[];
    warnings: string[];
  } {
    const errors: string[] = [];
    const warnings: string[] = [];

    // 필수 필드 검증
    if (!policy.name) {
      errors.push('정책 이름은 필수입니다.');
    }

    // 보존 기간 검증
    if (policy.dbRetentionDays !== undefined && policy.dbRetentionDays < 1) {
      errors.push('DB 보존 기간은 1일 이상이어야 합니다.');
    }

    if (policy.totalRetentionDays !== undefined && policy.totalRetentionDays < 1) {
      errors.push('전체 보존 기간은 1일 이상이어야 합니다.');
    }

    if (
      policy.dbRetentionDays !== undefined && 
      policy.totalRetentionDays !== undefined &&
      policy.dbRetentionDays > policy.totalRetentionDays
    ) {
      errors.push('DB 보존 기간은 전체 보존 기간보다 작거나 같아야 합니다.');
    }

    // 배치 크기 검증
    if (policy.batchSize !== undefined) {
      if (policy.batchSize < 1) {
        errors.push('배치 크기는 1 이상이어야 합니다.');
      } else if (policy.batchSize > 10000) {
        errors.push('배치 크기는 10,000 이하여야 합니다.');
      } else if (policy.batchSize > 5000) {
        warnings.push('배치 크기가 5,000을 초과하면 메모리 사용량이 클 수 있습니다.');
      }
    }

    // 경로 검증
    if (policy.archiveBasePath) {
      if (!policy.archiveBasePath.startsWith('/')) {
        errors.push('아카이브 경로는 절대 경로여야 합니다.');
      }
    }

    // Cron 표현식 검증
    if (policy.cronExpression) {
      const cronRegex = /^(\*|([0-9]|1[0-9]|2[0-9]|3[0-9]|4[0-9]|5[0-9])|\*\/([0-9]|1[0-9]|2[0-9]|3[0-9]|4[0-9]|5[0-9])) (\*|([0-9]|1[0-9]|2[0-3])|\*\/([0-9]|1[0-9]|2[0-3])) (\*|([1-9]|1[0-9]|2[0-9]|3[0-1])|\*\/([1-9]|1[0-9]|2[0-9]|3[0-1])) (\*|([1-9]|1[0-2])|\*\/([1-9]|1[0-2])) (\*|([0-6])|\*\/([0-6]))$/;
      
      if (!cronRegex.test(policy.cronExpression)) {
        errors.push('Cron 표현식 형식이 올바르지 않습니다.');
      }
    }

    return {
      isValid: errors.length === 0,
      errors,
      warnings,
    };
  }

  /**
   * 정책 적용 시뮬레이션
   */
  static async simulatePolicy(policy: UpdatePolicyRequest): Promise<{
    estimatedAffectedRecords: number;
    estimatedArchiveSize: number;
    estimatedExecutionTime: number;
    storageRequirement: number;
    potentialIssues: string[];
  }> {
    try {
      const response = await ApiService.post('/v1/policies/simulate', policy);
      return response.data;
    } catch (error) {
      // API가 없는 경우 기본값 반환
      return {
        estimatedAffectedRecords: 0,
        estimatedArchiveSize: 0,
        estimatedExecutionTime: 0,
        storageRequirement: 0,
        potentialIssues: [],
      };
    }
  }

  /**
   * 정책 실행 히스토리 조회
   */
  static async getPolicyExecutionHistory(
    policyId: number,
    params?: {
      page?: number;
      size?: number;
      startDate?: string;
      endDate?: string;
      status?: 'SUCCESS' | 'PARTIAL_SUCCESS' | 'FAILED';
    }
  ): Promise<{
    content: PolicyExecutionResult[];
    totalElements: number;
    totalPages: number;
    hasNext: boolean;
  }> {
    try {
      const response = await ApiService.get(`/v1/policies/${policyId}/executions`, params);
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
   * 다음 정책 실행 예정 시간 계산
   */
  static getNextExecutionTime(cronExpression: string): Date | null {
    try {
      // 실제 구현에서는 cron 파싱 라이브러리 사용
      // 여기서는 간단한 예시
      const now = new Date();
      
      // 매일 2시 실행 ("0 0 2 * * ?")의 경우
      if (cronExpression === "0 0 2 * * ?") {
        const nextExecution = new Date(now);
        nextExecution.setHours(2, 0, 0, 0);
        
        // 이미 2시를 지났다면 다음 날 2시
        if (nextExecution <= now) {
          nextExecution.setDate(nextExecution.getDate() + 1);
        }
        
        return nextExecution;
      }
      
      return null;
    } catch (error) {
      console.error('Failed to calculate next execution time:', error);
      return null;
    }
  }

  /**
   * Cron 표현식을 사람이 읽기 쉬운 형태로 변환
   */
  static parseCronExpression(cronExpression: string): string {
    const commonExpressions: Record<string, string> = {
      "0 0 2 * * ?": "매일 새벽 2시",
      "0 0 1 * * ?": "매일 새벽 1시",
      "0 0 3 * * ?": "매일 새벽 3시",
      "0 0 0 * * SUN": "매주 일요일 자정",
      "0 0 0 1 * ?": "매월 1일 자정",
    };

    return commonExpressions[cronExpression] || cronExpression;
  }

  /**
   * 정책 템플릿 목록 조회
   */
  static getPolicyTemplates(): Array<{
    name: string;
    description: string;
    policy: Partial<DataRetentionPolicy>;
  }> {
    return [
      {
        name: "기본 정책",
        description: "일반적인 로그 관리를 위한 기본 설정",
        policy: {
          name: "기본 데이터 보존 정책",
          dbRetentionDays: 90,
          totalRetentionDays: 365,
          batchSize: 1000,
          enableArchiving: true,
          enableDataDeletion: true,
          cronExpression: "0 0 2 * * ?",
        },
      },
      {
        name: "단기 보존 정책",
        description: "짧은 기간 동안만 데이터를 보존",
        policy: {
          name: "단기 데이터 보존 정책",
          dbRetentionDays: 30,
          totalRetentionDays: 90,
          batchSize: 2000,
          enableArchiving: true,
          enableDataDeletion: true,
          cronExpression: "0 0 1 * * ?",
        },
      },
      {
        name: "장기 보존 정책",
        description: "장기간 데이터 보존이 필요한 경우",
        policy: {
          name: "장기 데이터 보존 정책",
          dbRetentionDays: 180,
          totalRetentionDays: 1095, // 3년
          batchSize: 500,
          enableArchiving: true,
          enableDataDeletion: false,
          cronExpression: "0 0 3 * * SUN", // 주간 실행
        },
      },
      {
        name: "고성능 정책",
        description: "대량 데이터 처리를 위한 최적화된 설정",
        policy: {
          name: "고성능 데이터 보존 정책",
          dbRetentionDays: 60,
          totalRetentionDays: 365,
          batchSize: 5000,
          enableArchiving: true,
          enableDataDeletion: true,
          cronExpression: "0 0 2 * * ?",
        },
      },
    ];
  }
}