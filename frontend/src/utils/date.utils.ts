import { format, parseISO, isValid, startOfDay, endOfDay, subDays } from 'date-fns';
import { ko } from 'date-fns/locale';

// 기본 날짜 포맷
export const DEFAULT_DATE_FORMAT = 'yyyy-MM-dd';
export const DEFAULT_DATETIME_FORMAT = 'yyyy-MM-dd HH:mm:ss';
export const ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

// 날짜 포맷팅
export const formatDate = (date: string | Date, dateFormat = DEFAULT_DATE_FORMAT): string => {
  try {
    const parsedDate = typeof date === 'string' ? parseISO(date) : date;
    if (!isValid(parsedDate)) {
      return '유효하지 않은 날짜';
    }
    return format(parsedDate, dateFormat, { locale: ko });
  } catch {
    return '유효하지 않은 날짜';
  }
};

// 날짜시간 포맷팅
export const formatDateTime = (date: string | Date | null | undefined): string => {
  if (!date) return '알 수 없음';
  
  try {
    const parsedDate = typeof date === 'string' ? parseISO(date) : date;
    if (!isValid(parsedDate)) {
      console.warn('유효하지 않은 날짜:', date);
      return '유효하지 않은 날짜';
    }
    return format(parsedDate, DEFAULT_DATETIME_FORMAT, { locale: ko });
  } catch (error) {
    console.error('날짜 파싱 에러:', error, date);
    return '날짜 파싱 오류';
  }
};

// ISO 문자열로 변환
export const toISOString = (date: string | Date): string => {
  try {
    const parsedDate = typeof date === 'string' ? parseISO(date) : date;
    if (!isValid(parsedDate)) {
      throw new Error('Invalid date');
    }
    return parsedDate.toISOString();
  } catch {
    return new Date().toISOString();
  }
};

// 상대적 시간 표시
export const getRelativeTime = (date: string | Date): string => {
  try {
    const parsedDate = typeof date === 'string' ? parseISO(date) : date;
    const now = new Date();
    const diffInHours = Math.floor((now.getTime() - parsedDate.getTime()) / (1000 * 60 * 60));
    
    if (diffInHours < 1) {
      const diffInMinutes = Math.floor((now.getTime() - parsedDate.getTime()) / (1000 * 60));
      return diffInMinutes < 1 ? '방금 전' : `${diffInMinutes}분 전`;
    }
    if (diffInHours < 24) {
      return `${diffInHours}시간 전`;
    }
    const diffInDays = Math.floor(diffInHours / 24);
    if (diffInDays < 7) {
      return `${diffInDays}일 전`;
    }
    return formatDate(parsedDate);
  } catch {
    return '알 수 없음';
  }
};

// 날짜 범위 유효성 검사
export const isValidDateRange = (startDate: string | Date, endDate: string | Date): boolean => {
  try {
    const start = typeof startDate === 'string' ? parseISO(startDate) : startDate;
    const end = typeof endDate === 'string' ? parseISO(endDate) : endDate;
    
    return isValid(start) && isValid(end) && start <= end;
  } catch {
    return false;
  }
};

// 오늘 날짜 범위
export const getTodayRange = (): { start: string; end: string } => {
  const now = new Date();
  return {
    start: startOfDay(now).toISOString(),
    end: endOfDay(now).toISOString(),
  };
};

// 어제 날짜 범위
export const getYesterdayRange = (): { start: string; end: string } => {
  const yesterday = subDays(new Date(), 1);
  return {
    start: startOfDay(yesterday).toISOString(),
    end: endOfDay(yesterday).toISOString(),
  };
};

// 최근 N일 날짜 범위
export const getLastNDaysRange = (days: number): { start: string; end: string } => {
  const now = new Date();
  const startDate = subDays(now, days - 1);
  return {
    start: startOfDay(startDate).toISOString(),
    end: endOfDay(now).toISOString(),
  };
};

// 월 시작~끝 날짜 범위
export const getCurrentMonthRange = (): { start: string; end: string } => {
  const now = new Date();
  const year = now.getFullYear();
  const month = now.getMonth();
  
  return {
    start: new Date(year, month, 1).toISOString(),
    end: endOfDay(new Date(year, month + 1, 0)).toISOString(),
  };
};

// HTML input date 형식으로 변환 (yyyy-MM-dd)
export const toInputDate = (date: string | Date): string => {
  try {
    const parsedDate = typeof date === 'string' ? parseISO(date) : date;
    if (!isValid(parsedDate)) {
      return '';
    }
    return format(parsedDate, 'yyyy-MM-dd');
  } catch {
    return '';
  }
};

// HTML input datetime-local 형식으로 변환 (yyyy-MM-ddTHH:mm)
export const toInputDateTime = (date: string | Date): string => {
  try {
    const parsedDate = typeof date === 'string' ? parseISO(date) : date;
    if (!isValid(parsedDate)) {
      return '';
    }
    return format(parsedDate, "yyyy-MM-dd'T'HH:mm");
  } catch {
    return '';
  }
};

// 날짜 범위 프리셋
export interface DateRangePreset {
  label: string;
  getValue: () => { start: string; end: string };
}

export const DATE_RANGE_PRESETS: DateRangePreset[] = [
  {
    label: '오늘',
    getValue: getTodayRange,
  },
  {
    label: '어제',
    getValue: getYesterdayRange,
  },
  {
    label: '최근 7일',
    getValue: () => getLastNDaysRange(7),
  },
  {
    label: '최근 30일',
    getValue: () => getLastNDaysRange(30),
  },
  {
    label: '이번 달',
    getValue: getCurrentMonthRange,
  },
];