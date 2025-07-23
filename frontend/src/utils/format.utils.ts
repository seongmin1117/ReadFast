// 숫자 포맷팅 유틸리티
export const formatNumber = (value: number, locale = 'ko-KR'): string => {
  return new Intl.NumberFormat(locale).format(value);
};

// 파일 크기 포맷팅
export const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 Bytes';
  
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

// 실행 시간 포맷팅
export const formatDuration = (milliseconds: number): string => {
  if (milliseconds < 1000) {
    return `${milliseconds}ms`;
  }
  
  const seconds = Math.floor(milliseconds / 1000);
  if (seconds < 60) {
    return `${seconds}초`;
  }
  
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  
  if (minutes < 60) {
    return remainingSeconds > 0 ? `${minutes}분 ${remainingSeconds}초` : `${minutes}분`;
  }
  
  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;
  
  return remainingMinutes > 0 ? `${hours}시간 ${remainingMinutes}분` : `${hours}시간`;
};

// 백분율 포맷팅
export const formatPercentage = (value: number, decimals = 1): string => {
  return `${(value * 100).toFixed(decimals)}%`;
};

// 날짜시간 포맷팅 (date.utils에서 import하여 re-export)
export { formatDateTime } from './date.utils';

// 문자열 잘라내기
export const truncate = (text: string, maxLength: number, suffix = '...'): string => {
  if (text.length <= maxLength) return text;
  return text.substring(0, maxLength - suffix.length) + suffix;
};

// 카멜케이스를 사람이 읽기 좋은 형태로 변환
export const formatCamelCase = (text: string): string => {
  return text
    .replace(/([A-Z])/g, ' $1')
    .replace(/^./, (str) => str.toUpperCase())
    .trim();
};

// 스네이크케이스를 사람이 읽기 좋은 형태로 변환
export const formatSnakeCase = (text: string): string => {
  return text
    .split('_')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
    .join(' ');
};

// 상태 텍스트 한글화
export const formatStatus = (status: string): string => {
  const statusMap: Record<string, string> = {
    success: '성공',
    error: '에러',
    warning: '경고',
    info: '정보',
    loading: '로딩 중',
    pending: '대기 중',
    completed: '완료',
    failed: '실패',
    active: '활성',
    inactive: '비활성',
    enabled: '활성화됨',
    disabled: '비활성화됨',
  };
  
  return statusMap[status.toLowerCase()] || formatCamelCase(status);
};

// 디바이스 타입 포맷팅
export const formatDevice = (device: string): string => {
  const deviceMap: Record<string, string> = {
    mobile: '모바일',
    desktop: '데스크톱',
    tablet: '태블릿',
    web: '웹',
    ios: 'iOS',
    android: '안드로이드',
    windows: 'Windows',
    mac: 'Mac',
    linux: 'Linux',
  };
  
  return deviceMap[device.toLowerCase()] || device;
};

// 인증 결과 포맷팅
export const formatAuthResult = (result: string): string => {
  const resultMap: Record<string, string> = {
    success: '성공',
    failure: '실패',
    blocked: '차단',
    expired: '만료',
    invalid: '유효하지 않음',
    timeout: '시간 초과',
  };
  
  return resultMap[result.toLowerCase()] || result;
};

// API 엔드포인트 표시용 포맷팅
export const formatEndpoint = (endpoint: string): string => {
  // 너무 긴 엔드포인트는 축약
  if (endpoint.length > 50) {
    const parts = endpoint.split('/');
    if (parts.length > 3) {
      return `/${parts[1]}/.../${parts[parts.length - 1]}`;
    }
  }
  return endpoint;
};

// JSON 예쁘게 출력
export const formatJSON = (obj: any, indent = 2): string => {
  try {
    return JSON.stringify(obj, null, indent);
  } catch {
    return String(obj);
  }
};

// 검색어 하이라이트
export const highlightText = (text: string, searchTerm: string): string => {
  if (!searchTerm.trim()) return text;
  
  const regex = new RegExp(`(${searchTerm})`, 'gi');
  return text.replace(regex, '<mark>$1</mark>');
};

// 압축 비율 포맷팅
export const formatCompressionRatio = (ratio: number): string => {
  if (ratio >= 1) return '압축 없음';
  return `${((1 - ratio) * 100).toFixed(1)}% 압축`;
};

// 체크섬 축약 표시
export const formatChecksum = (checksum: string): string => {
  if (checksum.length <= 16) return checksum;
  return `${checksum.substring(0, 8)}...${checksum.substring(checksum.length - 8)}`;
};