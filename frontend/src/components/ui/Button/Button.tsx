import React from 'react';
import { cn } from '@/utils/cn.utils';
import type { Variant, Size } from '@/types/common.types';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant | 'outline' | 'ghost' | 'link';
  size?: Size;
  loading?: boolean;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
}

const buttonVariants = {
  primary: 'bg-blue-600 text-white hover:bg-blue-700 active:bg-blue-800 focus:ring-blue-500',
  secondary: 'bg-gray-100 text-gray-900 hover:bg-gray-200 active:bg-gray-300 focus:ring-gray-500',
  danger: 'bg-red-600 text-white hover:bg-red-700 active:bg-red-800 focus:ring-red-500',
  success: 'bg-green-600 text-white hover:bg-green-700 active:bg-green-800 focus:ring-green-500',
  warning: 'bg-yellow-600 text-white hover:bg-yellow-700 active:bg-yellow-800 focus:ring-yellow-500',
  outline: 'border border-gray-300 bg-transparent hover:bg-gray-50 active:bg-gray-100 focus:ring-gray-500',
  ghost: 'bg-transparent hover:bg-gray-100 active:bg-gray-200 focus:ring-gray-500',
  link: 'bg-transparent text-blue-600 underline-offset-4 hover:underline focus:ring-blue-500',
};

const buttonSizes = {
  sm: 'h-8 px-3 text-sm',
  md: 'h-10 px-4 py-2 text-sm',
  lg: 'h-11 px-6 text-base',
  xl: 'h-12 px-8 text-lg',
};

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ 
    className, 
    variant = 'primary', 
    size = 'md', 
    loading = false,
    leftIcon,
    rightIcon,
    children,
    disabled,
    ...props 
  }, ref) => {
    const baseClasses = 'inline-flex items-center justify-center rounded-md font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50';
    
    return (
      <button
        className={cn(
          baseClasses,
          buttonVariants[variant],
          buttonSizes[size],
          loading && 'cursor-wait',
          className
        )}
        ref={ref}
        disabled={disabled || loading}
        {...props}
      >
        {loading && (
          <svg
            className="mr-2 h-4 w-4 animate-spin"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
          >
            <circle
              className="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              strokeWidth="4"
            />
            <path
              className="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            />
          </svg>
        )}
        {!loading && leftIcon && (
          <span className="mr-2">{leftIcon}</span>
        )}
        {children}
        {!loading && rightIcon && (
          <span className="ml-2">{rightIcon}</span>
        )}
      </button>
    );
  }
);

Button.displayName = 'Button';