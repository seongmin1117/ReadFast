/**
 * 타임존 처리 유틸리티
 * 한국 시간대(Asia/Seoul) 기준으로 사용자에게 친화적인 시간 처리
 */

// 한국 시간대 정의
export const KOREA_TIMEZONE = 'Asia/Seoul';

/**
 * 한국시간 기준으로 Date 객체를 'YYYY-MM-DDTHH:mm' 형식으로 변환
 * datetime-local input에서 사용
 */
export function toKoreanDateTimeLocal(date: Date | string | null | undefined): string {
  if (!date) return '';
  
  const dateObj = typeof date === 'string' ? new Date(date) : date;
  if (isNaN(dateObj.getTime())) return '';
  
  // UTC 시간을 한국 시간으로 변환하여 datetime-local 형식으로 표시
  const koreanTime = new Date(dateObj.getTime() + (9 * 60 * 60 * 1000));
  
  const year = koreanTime.getUTCFullYear();
  const month = String(koreanTime.getUTCMonth() + 1).padStart(2, '0');
  const day = String(koreanTime.getUTCDate()).padStart(2, '0');
  const hour = String(koreanTime.getUTCHours()).padStart(2, '0');
  const minute = String(koreanTime.getUTCMinutes()).padStart(2, '0');
  
  return `${year}-${month}-${day}T${hour}:${minute}`;
}

/**
 * datetime-local input 값(한국시간 기준)을 UTC Date 객체로 변환
 * 서버로 전송할 때 사용
 */
export function fromKoreanDateTimeLocal(datetimeLocalValue: string): Date | null {
  if (!datetimeLocalValue) return null;
  
  try {
    // datetime-local 값을 한국시간으로 해석하고 UTC로 변환
    const localDate = new Date(datetimeLocalValue);
    
    // 사용자의 로컬 타임존에서 한국 타임존으로 보정
    const userOffset = localDate.getTimezoneOffset(); // 분 단위
    const koreaOffset = -540; // 한국은 UTC+9 (음수로 표현)
    const offsetDiff = userOffset - koreaOffset; // 분 단위 차이
    
    // 오프셋 차이를 적용하여 UTC 시간 계산
    const utcDate = new Date(localDate.getTime() + (offsetDiff * 60 * 1000));
    
    return utcDate;
  } catch (error) {
    console.error('날짜 변환 오류:', error);
    return null;
  }
}

/**
 * 한국시간 기준으로 현재 시간을 'YYYY-MM-DDTHH:mm' 형식으로 반환
 */
export function nowAsKoreanDateTimeLocal(): string {
  return toKoreanDateTimeLocal(new Date());
}

/**
 * Date 객체를 한국시간 기준으로 포맷팅
 */
export function formatKoreanDateTime(date: Date | string | null | undefined): string {
  if (!date) return '';
  
  const dateObj = typeof date === 'string' ? new Date(date) : date;
  if (isNaN(dateObj.getTime())) return '';
  
  return new Intl.DateTimeFormat('ko-KR', {
    timeZone: KOREA_TIMEZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).format(dateObj);
}

/**
 * Date 객체를 한국시간 기준으로 날짜만 포맷팅
 */
export function formatKoreanDate(date: Date | string | null | undefined): string {
  if (!date) return '';
  
  const dateObj = typeof date === 'string' ? new Date(date) : date;
  if (isNaN(dateObj.getTime())) return '';
  
  return new Intl.DateTimeFormat('ko-KR', {
    timeZone: KOREA_TIMEZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(dateObj);
}

/**
 * 시간대 정보를 표시하는 헬퍼
 */
export function getTimezoneInfo(): { 
  name: string; 
  offset: string; 
  displayName: string;
} {
  const now = new Date();
  const koreanTime = new Intl.DateTimeFormat('ko-KR', {
    timeZone: KOREA_TIMEZONE,
    timeZoneName: 'short'
  }).formatToParts(now);
  
  const timeZoneName = koreanTime.find(part => part.type === 'timeZoneName')?.value || 'KST';
  
  return {
    name: KOREA_TIMEZONE,
    offset: '+09:00',
    displayName: `한국시간 (${timeZoneName})`
  };
}