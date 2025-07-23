import { useState, useCallback, useMemo } from 'react';

interface UsePaginationOptions {
  initialPage?: number;
  initialPageSize?: number;
  pageSizeOptions?: number[];
  maxVisiblePages?: number;
}

interface UsePaginationReturn {
  // Current state
  currentPage: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
  
  // Page info
  startIndex: number;
  endIndex: number;
  isEmpty: boolean;
  
  // Visible pages for pagination UI
  visiblePages: number[];
  showFirstPage: boolean;
  showLastPage: boolean;
  showPreviousEllipsis: boolean;
  showNextEllipsis: boolean;
  
  // Actions
  goToPage: (page: number) => void;
  goToFirstPage: () => void;
  goToLastPage: () => void;
  goToNextPage: () => void;
  goToPreviousPage: () => void;
  changePageSize: (size: number) => void;
  setTotalElements: (total: number) => void;
  reset: () => void;
  
  // Page size options
  pageSizeOptions: number[];
}

const DEFAULT_PAGE_SIZE_OPTIONS = [10, 20, 50, 100];

export function usePagination(options: UsePaginationOptions = {}): UsePaginationReturn {
  const {
    initialPage = 0,
    initialPageSize = 20,
    pageSizeOptions = DEFAULT_PAGE_SIZE_OPTIONS,
    maxVisiblePages = 7,
  } = options;

  const [currentPage, setCurrentPage] = useState(initialPage);
  const [pageSize, setPageSize] = useState(initialPageSize);
  const [totalElements, setTotalElementsState] = useState(0);

  // Computed values
  const totalPages = Math.ceil(totalElements / pageSize);
  const hasNext = currentPage < totalPages - 1;
  const hasPrevious = currentPage > 0;
  const startIndex = currentPage * pageSize + 1;
  const endIndex = Math.min((currentPage + 1) * pageSize, totalElements);
  const isEmpty = totalElements === 0;

  // Visible pages calculation
  const visiblePages = useMemo(() => {
    if (totalPages <= maxVisiblePages) {
      return Array.from({ length: totalPages }, (_, i) => i);
    }

    const sidePages = Math.floor((maxVisiblePages - 1) / 2);
    let startPage = Math.max(0, currentPage - sidePages);
    let endPage = Math.min(totalPages - 1, currentPage + sidePages);

    // Adjust if we're near the beginning or end
    if (currentPage < sidePages) {
      endPage = Math.min(totalPages - 1, maxVisiblePages - 1);
    } else if (currentPage > totalPages - sidePages - 1) {
      startPage = Math.max(0, totalPages - maxVisiblePages);
    }

    return Array.from({ length: endPage - startPage + 1 }, (_, i) => startPage + i);
  }, [currentPage, totalPages, maxVisiblePages]);

  const showFirstPage = visiblePages[0] > 0;
  const showLastPage = visiblePages[visiblePages.length - 1] < totalPages - 1;
  const showPreviousEllipsis = showFirstPage && visiblePages[0] > 1;
  const showNextEllipsis = showLastPage && visiblePages[visiblePages.length - 1] < totalPages - 2;

  // Actions
  const goToPage = useCallback((page: number) => {
    if (page >= 0 && page < totalPages) {
      setCurrentPage(page);
    }
  }, [totalPages]);

  const goToFirstPage = useCallback(() => {
    setCurrentPage(0);
  }, []);

  const goToLastPage = useCallback(() => {
    if (totalPages > 0) {
      setCurrentPage(totalPages - 1);
    }
  }, [totalPages]);

  const goToNextPage = useCallback(() => {
    if (hasNext) {
      setCurrentPage(prev => prev + 1);
    }
  }, [hasNext]);

  const goToPreviousPage = useCallback(() => {
    if (hasPrevious) {
      setCurrentPage(prev => prev - 1);
    }
  }, [hasPrevious]);

  const changePageSize = useCallback((size: number) => {
    const newTotalPages = Math.ceil(totalElements / size);
    const maxPage = Math.max(0, newTotalPages - 1);
    
    setPageSize(size);
    
    // Adjust current page if necessary
    if (currentPage > maxPage) {
      setCurrentPage(maxPage);
    }
  }, [currentPage, totalElements]);

  const setTotalElements = useCallback((total: number) => {
    setTotalElementsState(total);
    
    // Adjust current page if total elements changed
    const newTotalPages = Math.ceil(total / pageSize);
    if (currentPage >= newTotalPages && newTotalPages > 0) {
      setCurrentPage(newTotalPages - 1);
    } else if (newTotalPages === 0) {
      setCurrentPage(0);
    }
  }, [currentPage, pageSize]);

  const reset = useCallback(() => {
    setCurrentPage(initialPage);
    setPageSize(initialPageSize);
    setTotalElementsState(0);
  }, [initialPage, initialPageSize]);

  return {
    // Current state
    currentPage,
    pageSize,
    totalElements,
    totalPages,
    hasNext,
    hasPrevious,
    
    // Page info
    startIndex,
    endIndex,
    isEmpty,
    
    // Visible pages for pagination UI
    visiblePages,
    showFirstPage,
    showLastPage,
    showPreviousEllipsis,
    showNextEllipsis,
    
    // Actions
    goToPage,
    goToFirstPage,
    goToLastPage,
    goToNextPage,
    goToPreviousPage,
    changePageSize,
    setTotalElements,
    reset,
    
    // Page size options
    pageSizeOptions,
  };
}

// 커서 기반 페이지네이션용 훅
interface UseCursorPaginationOptions<T> {
  getCursorKey: (item: T) => string | number;
  initialPageSize?: number;
  pageSizeOptions?: number[];
}

interface UseCursorPaginationReturn<T> {
  pageSize: number;
  hasNext: boolean;
  hasPrevious: boolean;
  cursors: Array<{ id: string | number; date?: string }>;
  
  // Actions
  goToNext: (lastItem: T) => { cursorId?: number; cursorDate?: string };
  goToPrevious: () => { cursorId?: number; cursorDate?: string } | null;
  changePageSize: (size: number) => void;
  reset: () => void;
  
  // Page size options
  pageSizeOptions: number[];
}

export function useCursorPagination<T>(
  options: UseCursorPaginationOptions<T>
): UseCursorPaginationReturn<T> {
  const {
    getCursorKey,
    initialPageSize = 20,
    pageSizeOptions = DEFAULT_PAGE_SIZE_OPTIONS,
  } = options;

  const [pageSize, setPageSize] = useState(initialPageSize);
  const [cursors, setCursors] = useState<Array<{ id: string | number; date?: string }>>([]);
  const [hasNext, setHasNext] = useState(false);

  const hasPrevious = cursors.length > 0;

  const goToNext = useCallback((lastItem: T) => {
    const cursorKey = getCursorKey(lastItem);
    const cursorDate = (lastItem as any).date; // Assume date property exists
    
    setCursors(prev => [...prev, { id: cursorKey, date: cursorDate }]);
    
    return {
      cursorId: typeof cursorKey === 'number' ? cursorKey : undefined,
      cursorDate,
    };
  }, [getCursorKey]);

  const goToPrevious = useCallback(() => {
    if (cursors.length === 0) return null;

    const newCursors = cursors.slice(0, -1);
    setCursors(newCursors);

    if (newCursors.length === 0) {
      return {}; // First page
    }

    const lastCursor = newCursors[newCursors.length - 1];
    return {
      cursorId: typeof lastCursor.id === 'number' ? lastCursor.id : undefined,
      cursorDate: lastCursor.date,
    };
  }, [cursors]);

  const changePageSize = useCallback((size: number) => {
    setPageSize(size);
  }, []);

  const reset = useCallback(() => {
    setCursors([]);
    setHasNext(false);
    setPageSize(initialPageSize);
  }, [initialPageSize]);

  return {
    pageSize,
    hasNext,
    hasPrevious,
    cursors,
    
    // Actions
    goToNext,
    goToPrevious,
    changePageSize,
    reset,
    
    // Page size options
    pageSizeOptions,
  };
}

// 페이지네이션 정보 표시용 유틸리티
export function getPaginationInfo(
  currentPage: number,
  pageSize: number,
  totalElements: number
): {
  start: number;
  end: number;
  total: number;
  text: string;
} {
  if (totalElements === 0) {
    return {
      start: 0,
      end: 0,
      total: 0,
      text: '검색 결과가 없습니다.',
    };
  }

  const start = currentPage * pageSize + 1;
  const end = Math.min((currentPage + 1) * pageSize, totalElements);

  return {
    start,
    end,
    total: totalElements,
    text: `${start.toLocaleString()}-${end.toLocaleString()} / ${totalElements.toLocaleString()}개`,
  };
}

// 페이지 크기 변경 시 현재 항목 위치 유지
export function calculateNewPageForItem(
  currentPage: number,
  currentPageSize: number,
  newPageSize: number,
  itemIndex: number
): number {
  const absoluteIndex = currentPage * currentPageSize + itemIndex;
  return Math.floor(absoluteIndex / newPageSize);
}