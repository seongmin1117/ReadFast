import { z } from 'zod';

// 기본 검증 스키마들
export const authSearchSchema = z.object({
  startDate: z.string().optional(),
  endDate: z.string().optional(),
  device: z.string().optional(),
  userId: z.string().optional(),
  result: z.string().optional(),
  endpoint: z.string().optional(),
  page: z.number().min(0).optional(),
  size: z.number().min(1).max(1000).optional(),
  sortBy: z.enum(['id', 'date', 'device', 'userId', 'result', 'endpoint']).optional(),
  direction: z.enum(['asc', 'desc']).optional(),
  cursorId: z.number().min(1).optional(),
  cursorDate: z.string().optional(),
}).refine(
  (data) => {
    if (data.startDate && data.endDate) {
      return new Date(data.startDate) <= new Date(data.endDate);
    }
    return true;
  },
  {
    message: '시작 날짜는 종료 날짜보다 이전이어야 합니다',
    path: ['dateRange'],
  }
);

// 정책 업데이트 스키마
export const updatePolicySchema = z.object({
  name: z.string().min(1, '정책 이름은 필수입니다').optional(),
  dbRetentionDays: z.number().min(1, 'DB 보존 일수는 1 이상이어야 합니다').optional(),
  totalRetentionDays: z.number().min(1, '전체 보존 일수는 1 이상이어야 합니다').optional(),
  batchSize: z.number().min(1, '배치 크기는 1 이상이어야 합니다').max(10000, '배치 크기는 10000 이하여야 합니다').optional(),
  archiveBasePath: z.string().min(1, '아카이브 경로는 필수입니다').optional(),
  archiveFileFormat: z.string().min(1, '파일 형식은 필수입니다').optional(),
  enableArchiving: z.boolean().optional(),
  enableDataDeletion: z.boolean().optional(),
  cronExpression: z.string().regex(/^(\*|([0-9]|1[0-9]|2[0-9]|3[0-9]|4[0-9]|5[0-9])|\*\/([0-9]|1[0-9]|2[0-9]|3[0-9]|4[0-9]|5[0-9])) (\*|([0-9]|1[0-9]|2[0-3])|\*\/([0-9]|1[0-9]|2[0-3])) (\*|([1-9]|1[0-9]|2[0-9]|3[0-1])|\*\/([1-9]|1[0-9]|2[0-9]|3[0-1])) (\*|([1-9]|1[0-2])|\*\/([1-9]|1[0-2])) (\*|([0-6])|\*\/([0-6]))$/, 'Cron 표현식 형식이 올바르지 않습니다').optional(),
}).refine(
  (data) => {
    if (data.dbRetentionDays && data.totalRetentionDays) {
      return data.dbRetentionDays <= data.totalRetentionDays;
    }
    return true;
  },
  {
    message: 'DB 보존 기간은 전체 보존 기간보다 짧아야 합니다',
    path: ['retention'],
  }
);

// 이메일 검증
export const isValidEmail = (email: string): boolean => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

// 전화번호 검증 (한국)
export const isValidPhoneNumber = (phone: string): boolean => {
  const phoneRegex = /^(\+82|0)?1[0-9]{1}-?[0-9]{4}-?[0-9]{4}$/;
  return phoneRegex.test(phone.replace(/\s/g, ''));
};

// URL 검증
export const isValidUrl = (url: string): boolean => {
  try {
    new URL(url);
    return true;
  } catch {
    return false;
  }
};

// 날짜 범위 검증
export const validateDateRange = (startDate: string, endDate: string): string | null => {
  if (!startDate || !endDate) return null;
  
  const start = new Date(startDate);
  const end = new Date(endDate);
  
  if (isNaN(start.getTime()) || isNaN(end.getTime())) {
    return '유효하지 않은 날짜 형식입니다';
  }
  
  if (start > end) {
    return '시작 날짜는 종료 날짜보다 이전이어야 합니다';
  }
  
  // 최대 1년 범위 제한
  const maxRange = 365 * 24 * 60 * 60 * 1000; // 1년
  if (end.getTime() - start.getTime() > maxRange) {
    return '날짜 범위는 최대 1년까지 선택할 수 있습니다';
  }
  
  return null;
};

// 파일 크기 검증
export const validateFileSize = (file: File, maxSizeMB = 10): string | null => {
  const maxSizeBytes = maxSizeMB * 1024 * 1024;
  if (file.size > maxSizeBytes) {
    return `파일 크기는 ${maxSizeMB}MB 이하여야 합니다`;
  }
  return null;
};

// 파일 타입 검증
export const validateFileType = (file: File, allowedTypes: string[]): string | null => {
  if (!allowedTypes.includes(file.type)) {
    return `허용되지 않는 파일 형식입니다. 허용: ${allowedTypes.join(', ')}`;
  }
  return null;
};

// 비밀번호 강도 검증
export const validatePasswordStrength = (password: string): {
  score: number;
  feedback: string[];
  isValid: boolean;
} => {
  const feedback: string[] = [];
  let score = 0;
  
  if (password.length >= 8) {
    score += 1;
  } else {
    feedback.push('8자 이상이어야 합니다');
  }
  
  if (/[a-z]/.test(password)) {
    score += 1;
  } else {
    feedback.push('소문자를 포함해야 합니다');
  }
  
  if (/[A-Z]/.test(password)) {
    score += 1;
  } else {
    feedback.push('대문자를 포함해야 합니다');
  }
  
  if (/\d/.test(password)) {
    score += 1;
  } else {
    feedback.push('숫자를 포함해야 합니다');
  }
  
  if (/[^a-zA-Z0-9]/.test(password)) {
    score += 1;
  } else {
    feedback.push('특수문자를 포함해야 합니다');
  }
  
  return {
    score,
    feedback,
    isValid: score >= 4,
  };
};

// Cron 표현식 검증
export const validateCronExpression = (cron: string): string | null => {
  const cronRegex = /^(\*|([0-9]|1[0-9]|2[0-9]|3[0-9]|4[0-9]|5[0-9])|\*\/([0-9]|1[0-9]|2[0-9]|3[0-9]|4[0-9]|5[0-9])) (\*|([0-9]|1[0-9]|2[0-3])|\*\/([0-9]|1[0-9]|2[0-3])) (\*|([1-9]|1[0-9]|2[0-9]|3[0-1])|\*\/([1-9]|1[0-9]|2[0-9]|3[0-1])) (\*|([1-9]|1[0-2])|\*\/([1-9]|1[0-2])) (\*|([0-6])|\*\/([0-6]))$/;
  
  if (!cronRegex.test(cron)) {
    return 'Cron 표현식 형식이 올바르지 않습니다 (예: 0 0 2 * * ?)';
  }
  
  return null;
};

// 입력값 정리 유틸리티
export const sanitizeInput = (input: string): string => {
  return input.trim().replace(/[<>"/\\]/g, '');
};

// 숫자 범위 검증
export const validateNumberRange = (value: number, min: number, max: number): string | null => {
  if (value < min) {
    return `값은 ${min} 이상이어야 합니다`;
  }
  if (value > max) {
    return `값은 ${max} 이하여야 합니다`;
  }
  return null;
};

// 배치 크기 검증
export const validateBatchSize = (size: number): string | null => {
  return validateNumberRange(size, 1, 10000);
};

// 보존 기간 검증
export const validateRetentionDays = (dbDays: number, totalDays: number): string | null => {
  if (dbDays > totalDays) {
    return 'DB 보존 기간은 전체 보존 기간보다 짧아야 합니다';
  }
  if (dbDays < 1 || totalDays < 1) {
    return '보존 기간은 1일 이상이어야 합니다';
  }
  return null;
};