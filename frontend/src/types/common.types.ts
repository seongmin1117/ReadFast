// 기본 공통 타입들
export type Status = 'success' | 'error' | 'warning' | 'info' | 'loading';

export type Size = 'sm' | 'md' | 'lg' | 'xl';

export type Variant = 'primary' | 'secondary' | 'danger' | 'success' | 'warning' | 'ghost';

// 테이블 컬럼 타입
export interface TableColumn<T = any> {
  key: keyof T | string;
  title: string;
  width?: string | number;
  sortable?: boolean;
  filterable?: boolean;
  render?: (value: any, record: T, index: number) => React.ReactNode;
  align?: 'left' | 'center' | 'right';
}

// 폼 필드 타입
export interface FormField {
  name: string;
  label: string;
  type: 'text' | 'email' | 'password' | 'number' | 'date' | 'datetime-local' | 'select' | 'textarea';
  required?: boolean;
  placeholder?: string;
  options?: Array<{ label: string; value: string | number }>;
  validation?: Record<string, any>;
}

// 메뉴 아이템 타입
export interface MenuItem {
  id: string;
  label: string;
  icon?: string;
  path?: string;
  children?: MenuItem[];
  badge?: string | number;
  disabled?: boolean;
}

// 알림 타입
export interface Notification {
  id: string;
  type: Status;
  title: string;
  message: string;
  duration?: number;
  closable?: boolean;
  timestamp: Date;
}

// 테마 타입
export interface Theme {
  mode: 'light' | 'dark';
  primaryColor: string;
  colors: Record<string, string>;
}

// 사용자 설정 타입
export interface UserSettings {
  theme: Theme;
  language: 'ko' | 'en';
  dateFormat: string;
  timezone: string;
  notifications: boolean;
}

// 브레드크럼 타입
export interface Breadcrumb {
  label: string;
  path?: string;
  icon?: string;
}

// 차트 데이터 타입
export interface ChartData {
  labels: string[];
  datasets: Array<{
    label: string;
    data: number[];
    backgroundColor?: string | string[];
    borderColor?: string | string[];
    borderWidth?: number;
  }>;
}

// 통계 카드 타입
export interface StatCard {
  title: string;
  value: string | number;
  change?: {
    value: string | number;
    trend: 'up' | 'down' | 'neutral';
  };
  icon?: string;
  color?: Variant;
}