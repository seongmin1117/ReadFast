import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

// Tailwind CSS 클래스 병합 유틸리티
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

// 상태별 색상 클래스 반환
export const getStatusClass = (status: string): string => {
  const statusClasses: Record<string, string> = {
    success: 'text-green-600 bg-green-50 border-green-200',
    error: 'text-red-600 bg-red-50 border-red-200',
    warning: 'text-yellow-600 bg-yellow-50 border-yellow-200',
    info: 'text-blue-600 bg-blue-50 border-blue-200',
    loading: 'text-gray-600 bg-gray-50 border-gray-200',
    pending: 'text-orange-600 bg-orange-50 border-orange-200',
  };
  
  return statusClasses[status] || statusClasses.info;
};

// 버튼 variant 클래스 반환
export const getButtonClass = (variant: string, size: string = 'md'): string => {
  const variants: Record<string, string> = {
    primary: 'bg-blue-600 hover:bg-blue-700 text-white border-blue-600',
    secondary: 'bg-gray-100 hover:bg-gray-200 text-gray-900 border-gray-300',
    danger: 'bg-red-600 hover:bg-red-700 text-white border-red-600',
    success: 'bg-green-600 hover:bg-green-700 text-white border-green-600',
    warning: 'bg-yellow-600 hover:bg-yellow-700 text-white border-yellow-600',
    ghost: 'hover:bg-gray-100 text-gray-700 border-transparent',
  };
  
  const sizes: Record<string, string> = {
    sm: 'px-3 py-1.5 text-sm',
    md: 'px-4 py-2 text-sm',
    lg: 'px-6 py-3 text-base',
    xl: 'px-8 py-4 text-lg',
  };
  
  const baseClass = 'inline-flex items-center justify-center font-medium rounded-md border transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:pointer-events-none';
  
  return cn(baseClass, variants[variant] || variants.primary, sizes[size] || sizes.md);
};

// 배지 클래스 반환
export const getBadgeClass = (variant: string): string => {
  const variants: Record<string, string> = {
    primary: 'bg-blue-100 text-blue-800 border-blue-200',
    secondary: 'bg-gray-100 text-gray-800 border-gray-200',
    success: 'bg-green-100 text-green-800 border-green-200',
    danger: 'bg-red-100 text-red-800 border-red-200',
    warning: 'bg-yellow-100 text-yellow-800 border-yellow-200',
    info: 'bg-blue-100 text-blue-800 border-blue-200',
  };
  
  const baseClass = 'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border';
  
  return cn(baseClass, variants[variant] || variants.primary);
};

// 인풋 상태별 클래스 반환
export const getInputClass = (error?: boolean, disabled?: boolean): string => {
  const baseClass = 'block w-full rounded-md border px-3 py-2 text-sm placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-offset-2 transition-colors';
  
  if (disabled) {
    return cn(baseClass, 'bg-gray-50 text-gray-500 cursor-not-allowed border-gray-200');
  }
  
  if (error) {
    return cn(baseClass, 'border-red-300 text-red-900 placeholder-red-300 focus:ring-red-500 focus:border-red-500');
  }
  
  return cn(baseClass, 'border-gray-300 focus:ring-blue-500 focus:border-blue-500');
};

// 테이블 행 호버 클래스
export const getTableRowClass = (isSelected?: boolean, isClickable?: boolean): string => {
  const baseClass = 'border-b border-gray-200';
  
  const classes = [baseClass];
  
  if (isSelected) {
    classes.push('bg-blue-50');
  }
  
  if (isClickable) {
    classes.push('hover:bg-gray-50 cursor-pointer');
  }
  
  return cn(...classes);
};

// 로딩 스피너 클래스
export const getSpinnerClass = (size: string = 'md'): string => {
  const sizes: Record<string, string> = {
    sm: 'w-4 h-4',
    md: 'w-6 h-6',
    lg: 'w-8 h-8',
    xl: 'w-12 h-12',
  };
  
  const baseClass = 'animate-spin rounded-full border-2 border-gray-300 border-t-blue-600';
  
  return cn(baseClass, sizes[size] || sizes.md);
};