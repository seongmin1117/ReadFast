import React from 'react';
import { cn } from '@/utils/cn.utils';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
  containerClassName?: string;
}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ 
    className, 
    type = 'text',
    label,
    error,
    helperText,
    leftIcon,
    rightIcon,
    containerClassName,
    id,
    ...props 
  }, ref) => {
    const inputId = id || label?.toLowerCase().replace(/\s+/g, '-');
    
    return (
      <div className={cn('w-full', containerClassName)}>
        {label && (
          <label 
            htmlFor={inputId}
            className="block text-sm font-medium text-gray-700 mb-2"
          >
            {label}
          </label>
        )}
        
        <div className="relative">
          {leftIcon && (
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <span className="text-gray-400">{leftIcon}</span>
            </div>
          )}
          
          <input
            type={type}
            id={inputId}
            className={cn(
              'block w-full rounded-md border border-gray-300 px-3 py-2 text-sm placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors',
              leftIcon && 'pl-10',
              rightIcon && 'pr-10',
              error && 'border-red-300 text-red-900 placeholder-red-300 focus:ring-red-500 focus:border-red-500',
              props.disabled && 'bg-gray-50 text-gray-500 cursor-not-allowed',
              className
            )}
            ref={ref}
            aria-invalid={error ? 'true' : 'false'}
            aria-describedby={
              error ? `${inputId}-error` : 
              helperText ? `${inputId}-helper` : 
              undefined
            }
            {...props}
          />
          
          {rightIcon && (
            <div className="absolute inset-y-0 right-0 pr-3 flex items-center">
              <span className="text-gray-400">{rightIcon}</span>
            </div>
          )}
        </div>
        
        {error && (
          <p id={`${inputId}-error`} className="mt-2 text-sm text-red-600">
            {error}
          </p>
        )}
        
        {!error && helperText && (
          <p id={`${inputId}-helper`} className="mt-2 text-sm text-gray-500">
            {helperText}
          </p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';