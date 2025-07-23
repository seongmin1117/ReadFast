import React, { useState, useRef, useEffect } from 'react';
import { Calendar, ChevronDown } from 'lucide-react';
import { cn } from '@/utils/cn.utils';
import { formatDate, toInputDate, DATE_RANGE_PRESETS } from '@/utils/date.utils';
import { Button } from '../Button';

interface DatePickerProps {
  value?: string;
  onChange: (date: string) => void;
  placeholder?: string;
  label?: string;
  error?: string;
  disabled?: boolean;
  className?: string;
  minDate?: string;
  maxDate?: string;
}

export const DatePicker: React.FC<DatePickerProps> = ({
  value,
  onChange,
  placeholder = '날짜 선택',
  label,
  error,
  disabled = false,
  className,
  minDate,
  maxDate,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onChange(e.target.value);
  };

  const displayValue = value ? formatDate(value) : '';

  return (
    <div className={cn('relative w-full', className)} ref={containerRef}>
      {label && (
        <label className="block text-sm font-medium text-gray-700 mb-2">
          {label}
        </label>
      )}
      
      <div className="relative">
        <input
          ref={inputRef}
          type="text"
          value={displayValue}
          placeholder={placeholder}
          disabled={disabled}
          readOnly
          className={cn(
            'block w-full rounded-md border border-gray-300 px-3 py-2 pr-10 text-sm placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 cursor-pointer',
            error && 'border-red-300 focus:ring-red-500 focus:border-red-500',
            disabled && 'bg-gray-50 text-gray-500 cursor-not-allowed'
          )}
          onClick={() => !disabled && setIsOpen(!isOpen)}
        />
        
        <div className="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none">
          <Calendar className="w-4 h-4 text-gray-400" />
        </div>
        
        {/* Hidden input for actual date value */}
        <input
          type="date"
          value={value ? toInputDate(value) : ''}
          onChange={handleInputChange}
          min={minDate ? toInputDate(minDate) : undefined}
          max={maxDate ? toInputDate(maxDate) : undefined}
          className="sr-only"
          disabled={disabled}
        />
      </div>

      {/* Dropdown */}
      {isOpen && !disabled && (
        <div className="absolute top-full left-0 right-0 z-10 mt-1 bg-white border border-gray-300 rounded-md shadow-lg">
          <div className="p-3">
            <input
              type="date"
              value={value ? toInputDate(value) : ''}
              onChange={handleInputChange}
              min={minDate ? toInputDate(minDate) : undefined}
              max={maxDate ? toInputDate(maxDate) : undefined}
              className="w-full p-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        </div>
      )}
      
      {error && (
        <p className="mt-2 text-sm text-red-600">
          {error}
        </p>
      )}
    </div>
  );
};

// DateRangePicker 컴포넌트
interface DateRangePickerProps {
  startDate?: string;
  endDate?: string;
  onStartDateChange: (date: string) => void;
  onEndDateChange: (date: string) => void;
  label?: string;
  error?: string;
  disabled?: boolean;
  className?: string;
  showPresets?: boolean;
}

export const DateRangePicker: React.FC<DateRangePickerProps> = ({
  startDate,
  endDate,
  onStartDateChange,
  onEndDateChange,
  label,
  error,
  disabled = false,
  className,
  showPresets = true,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handlePresetClick = (preset: typeof DATE_RANGE_PRESETS[0]) => {
    const range = preset.getValue();
    onStartDateChange(range.start);
    onEndDateChange(range.end);
    setIsOpen(false);
  };

  const displayValue = () => {
    if (startDate && endDate) {
      return `${formatDate(startDate)} ~ ${formatDate(endDate)}`;
    }
    if (startDate) {
      return `${formatDate(startDate)} ~`;
    }
    if (endDate) {
      return `~ ${formatDate(endDate)}`;
    }
    return '';
  };

  return (
    <div className={cn('relative w-full', className)} ref={containerRef}>
      {label && (
        <label className="block text-sm font-medium text-gray-700 mb-2">
          {label}
        </label>
      )}
      
      <div className="relative">
        <input
          type="text"
          value={displayValue()}
          placeholder="날짜 범위 선택"
          disabled={disabled}
          readOnly
          className={cn(
            'block w-full rounded-md border border-gray-300 px-3 py-2 pr-10 text-sm placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 cursor-pointer',
            error && 'border-red-300 focus:ring-red-500 focus:border-red-500',
            disabled && 'bg-gray-50 text-gray-500 cursor-not-allowed'
          )}
          onClick={() => !disabled && setIsOpen(!isOpen)}
        />
        
        <div className="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none">
          <ChevronDown className="w-4 h-4 text-gray-400" />
        </div>
      </div>

      {/* Dropdown */}
      {isOpen && !disabled && (
        <div className="absolute top-full left-0 right-0 z-10 mt-1 bg-white border border-gray-300 rounded-md shadow-lg">
          <div className="p-4">
            {showPresets && (
              <div className="mb-4">
                <p className="text-sm font-medium text-gray-700 mb-2">빠른 선택</p>
                <div className="flex flex-wrap gap-2">
                  {DATE_RANGE_PRESETS.map((preset) => (
                    <Button
                      key={preset.label}
                      variant="outline"
                      size="sm"
                      onClick={() => handlePresetClick(preset)}
                    >
                      {preset.label}
                    </Button>
                  ))}
                </div>
              </div>
            )}
            
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  시작일
                </label>
                <input
                  type="date"
                  value={startDate ? toInputDate(startDate) : ''}
                  onChange={(e) => onStartDateChange(e.target.value ? new Date(e.target.value).toISOString() : '')}
                  max={endDate ? toInputDate(endDate) : undefined}
                  className="w-full p-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  종료일
                </label>
                <input
                  type="date"
                  value={endDate ? toInputDate(endDate) : ''}
                  onChange={(e) => onEndDateChange(e.target.value ? new Date(e.target.value).toISOString() : '')}
                  min={startDate ? toInputDate(startDate) : undefined}
                  className="w-full p-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>
          </div>
        </div>
      )}
      
      {error && (
        <p className="mt-2 text-sm text-red-600">
          {error}
        </p>
      )}
    </div>
  );
};