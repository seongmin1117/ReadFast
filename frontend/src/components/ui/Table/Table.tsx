import React from 'react';
import { cn } from '@/utils/cn.utils';
import type { TableColumn } from '@/types/common.types';

interface TableProps<T = any> {
  columns: TableColumn<T>[];
  data: T[];
  loading?: boolean;
  className?: string;
  onRowClick?: (record: T, index: number) => void;
  selectedRows?: number[];
  emptyText?: string;
}

export function Table<T = any>({ 
  columns, 
  data, 
  loading = false,
  className,
  onRowClick,
  selectedRows = [],
  emptyText = '데이터가 없습니다.'
}: TableProps<T>) {
  const handleRowClick = (record: T, index: number) => {
    if (onRowClick) {
      onRowClick(record, index);
    }
  };

  if (loading) {
    return (
      <div className="w-full">
        <div className="animate-pulse">
          <div className="h-12 bg-gray-200 rounded mb-4"></div>
          {[...Array(5)].map((_, i) => (
            <div key={i} className="h-16 bg-gray-100 rounded mb-2"></div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className={cn('w-full overflow-hidden rounded-lg border border-gray-200', className)}>
      <div className="overflow-x-auto">
        <table className="w-full table-auto">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              {columns.map((column) => (
                <th
                  key={column.key as string}
                  className={cn(
                    'px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider',
                    column.align === 'center' && 'text-center',
                    column.align === 'right' && 'text-right'
                  )}
                  style={{ width: column.width }}
                >
                  {column.title}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {data.length === 0 ? (
              <tr>
                <td 
                  colSpan={columns.length}
                  className="px-4 py-8 text-center text-gray-500"
                >
                  {emptyText}
                </td>
              </tr>
            ) : (
              data.map((record, rowIndex) => (
                <tr
                  key={rowIndex}
                  className={cn(
                    'hover:bg-gray-50 transition-colors',
                    selectedRows.includes(rowIndex) && 'bg-blue-50',
                    onRowClick && 'cursor-pointer'
                  )}
                  onClick={() => handleRowClick(record, rowIndex)}
                >
                  {columns.map((column, colIndex) => {
                    const value = record[column.key as keyof T];
                    const cellContent = column.render 
                      ? column.render(value, record, rowIndex)
                      : String(value || '');

                    return (
                      <td
                        key={`${rowIndex}-${colIndex}`}
                        className={cn(
                          'px-4 py-3 text-sm text-gray-900',
                          column.align === 'center' && 'text-center',
                          column.align === 'right' && 'text-right'
                        )}
                      >
                        {cellContent}
                      </td>
                    );
                  })}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// Table 하위 컴포넌트들
export const TableHeader = React.forwardRef<
  HTMLTableSectionElement,
  React.HTMLAttributes<HTMLTableSectionElement>
>(({ className, ...props }, ref) => (
  <thead ref={ref} className={cn('bg-gray-50 border-b', className)} {...props} />
));

export const TableBody = React.forwardRef<
  HTMLTableSectionElement,
  React.HTMLAttributes<HTMLTableSectionElement>
>(({ className, ...props }, ref) => (
  <tbody ref={ref} className={cn('bg-white divide-y divide-gray-200', className)} {...props} />
));

export const TableRow = React.forwardRef<
  HTMLTableRowElement,
  React.HTMLAttributes<HTMLTableRowElement> & {
    selected?: boolean;
    clickable?: boolean;
  }
>(({ className, selected, clickable, ...props }, ref) => (
  <tr
    ref={ref}
    className={cn(
      'hover:bg-gray-50 transition-colors',
      selected && 'bg-blue-50',
      clickable && 'cursor-pointer',
      className
    )}
    {...props}
  />
));

export const TableHead = React.forwardRef<
  HTMLTableCellElement,
  React.ThHTMLAttributes<HTMLTableCellElement>
>(({ className, ...props }, ref) => (
  <th
    ref={ref}
    className={cn(
      'px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider',
      className
    )}
    {...props}
  />
));

export const TableCell = React.forwardRef<
  HTMLTableCellElement,
  React.TdHTMLAttributes<HTMLTableCellElement>
>(({ className, ...props }, ref) => (
  <td
    ref={ref}
    className={cn('px-4 py-3 text-sm text-gray-900', className)}
    {...props}
  />
));

TableHeader.displayName = 'TableHeader';
TableBody.displayName = 'TableBody';
TableRow.displayName = 'TableRow';
TableHead.displayName = 'TableHead';
TableCell.displayName = 'TableCell';