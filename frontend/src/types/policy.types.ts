// 데이터 보존 정책 타입
export interface DataRetentionPolicy {
  id: number;
  name: string;
  dbRetentionDays: number;
  totalRetentionDays: number;
  batchSize: number;
  archiveBasePath: string;
  archiveFileFormat: string;
  enableArchiving: boolean;
  enableDataDeletion: boolean;
  cronExpression: string;
  createdAt: string;
  updatedAt: string;
}

// 정책 업데이트 요청 타입
export interface UpdatePolicyRequest {
  name?: string;
  dbRetentionDays?: number;
  totalRetentionDays?: number;
  batchSize?: number;
  archiveBasePath?: string;
  archiveFileFormat?: string;
  enableArchiving?: boolean;
  enableDataDeletion?: boolean;
  cronExpression?: string;
}

// 아카이빙 전략 타입
export type ArchivingStrategy = 'FILE_BASED' | 'DATABASE_BASED' | 'CLOUD_STORAGE';

// 보존 규칙 타입
export interface RetentionRule {
  field: string;
  condition: 'older_than' | 'equal_to' | 'greater_than' | 'less_than';
  value: number | string;
  unit?: 'days' | 'months' | 'years';
}

// 아카이브 메타데이터 타입
export interface ArchiveMetadata {
  id: number;
  fileName: string;
  filePath: string;
  archiveDate: string;
  recordCount: number;
  fileSizeBytes: number;
  compressionRatio?: number;
  checksum: string;
  status: 'CREATED' | 'VERIFIED' | 'CORRUPTED' | 'DELETED';
  createdAt: string;
  updatedAt?: string;
}

// 정책 실행 결과 타입
export interface PolicyExecutionResult {
  policyId: number;
  executionDate: string;
  recordsProcessed: number;
  recordsArchived: number;
  recordsDeleted: number;
  executionTimeMs: number;
  status: 'SUCCESS' | 'PARTIAL_SUCCESS' | 'FAILED';
  errorMessage?: string;
  createdArchives: ArchiveMetadata[];
}

// 정책 설정 폼 타입
export interface PolicySettingsForm {
  general: {
    name: string;
    enableArchiving: boolean;
    enableDataDeletion: boolean;
  };
  retention: {
    dbRetentionDays: number;
    totalRetentionDays: number;
  };
  archiving: {
    archiveBasePath: string;
    archiveFileFormat: string;
    batchSize: number;
  };
  schedule: {
    cronExpression: string;
    nextExecution?: string;
  };
}