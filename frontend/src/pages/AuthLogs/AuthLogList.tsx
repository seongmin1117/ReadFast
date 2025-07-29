import React, {useState} from 'react';
import {Activity, Clock, RefreshCw, Search, Smartphone, User} from 'lucide-react';
import {Card, PageContainer} from '@/components/layout';
import {Button, Input, Modal, ModalBody, ModalFooter, Table} from '@/components/ui';
import {useAuthLogs} from '@/hooks/useAuthLogs';
import {formatAuthResult, formatDateTime, formatDevice, formatEndpoint} from '@/utils/format.utils';
import {cn} from '@/utils/cn.utils';
import {
  fromKoreanDateTimeLocal,
  getTimezoneInfo,
  toKoreanDateTimeLocal
} from '@/utils/timezone.utils';
import type {AuthLog, AuthSearchCondition, TableColumn} from '@/types';

// 검색 필터 컴포넌트
const SearchFilters: React.FC<{
  condition: AuthSearchCondition;
  onConditionChange: (updates: Partial<AuthSearchCondition>) => void;
  onSearch: () => void;
  onReset: () => void;
  isLoading: boolean;
}> = ({ condition, onConditionChange, onSearch, onReset, isLoading }) => {
  return (
    <Card title="검색 조건" className="mb-6">
      <div className="space-y-4">
        {/* 날짜 범위 */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              시작 날짜
              <span className="text-xs text-gray-500 ml-1">
                ({getTimezoneInfo().displayName})
              </span>
            </label>
            <Input
              type="datetime-local"
              value={toKoreanDateTimeLocal(condition.startDate)}
              onChange={(e) => onConditionChange({ 
                startDate: fromKoreanDateTimeLocal(e.target.value) || undefined 
              })}
              placeholder="시작 날짜 및 시간 선택"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              종료 날짜
              <span className="text-xs text-gray-500 ml-1">
                ({getTimezoneInfo().displayName})
              </span>
            </label>
            <Input
              type="datetime-local"
              value={toKoreanDateTimeLocal(condition.endDate)}
              onChange={(e) => onConditionChange({ 
                endDate: fromKoreanDateTimeLocal(e.target.value) || undefined 
              })}
              placeholder="종료 날짜 및 시간 선택"
            />
          </div>
        </div>

        {/* 기타 필터 */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              디바이스
            </label>
            <Input
              placeholder="디바이스 타입"
              value={condition.device || ''}
              onChange={(e) => onConditionChange({ device: e.target.value || undefined })}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              사용자 ID
            </label>
            <Input
              placeholder="사용자 ID"
              value={condition.userId || ''}
              onChange={(e) => onConditionChange({ userId: e.target.value || undefined })}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              인증 결과
            </label>
            <select 
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={condition.result || ''}
              onChange={(e) => onConditionChange({ result: e.target.value || undefined })}
            >
              <option value="">전체</option>
              <option value="success">성공</option>
              <option value="failure">실패</option>
              <option value="blocked">차단</option>
              <option value="expired">만료</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              엔드포인트
            </label>
            <Input
              placeholder="API 엔드포인트"
              value={condition.endpoint || ''}
              onChange={(e) => onConditionChange({ endpoint: e.target.value || undefined })}
            />
          </div>
        </div>

        {/* 검색 버튼 */}
        <div className="flex gap-2">
          <Button 
            onClick={onSearch} 
            disabled={isLoading}
            className="flex items-center gap-2"
          >
            <Search className="w-4 h-4" />
            {isLoading ? '검색 중...' : '검색'}
          </Button>
          <Button 
            variant="outline" 
            onClick={onReset}
            disabled={isLoading}
          >
            초기화
          </Button>
        </div>
      </div>
    </Card>
  );
};

// 상세 정보 모달
const AuthLogDetailModal: React.FC<{
  log: AuthLog | null;
  isOpen: boolean;
  onClose: () => void;
}> = ({ log, isOpen, onClose }) => {
  if (!log) return null;

  return (
    <Modal isOpen={isOpen} onClose={onClose} size="lg">
      <ModalBody>
        <div className="space-y-4">
          <h3 className="text-lg font-semibold text-gray-900">
            인증 로그 상세 정보
          </h3>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-500">ID</label>
              <p className="mt-1 text-sm text-gray-900">{log.id}</p>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-500">
                날짜 ({getTimezoneInfo().displayName})
              </label>
              <p className="mt-1 text-sm text-gray-900">{formatDateTime(log.date)}</p>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-500">디바이스</label>
              <p className="mt-1 text-sm text-gray-900">{formatDevice(log.device)}</p>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-500">사용자 ID</label>
              <p className="mt-1 text-sm text-gray-900">{log.userId}</p>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-500">인증 결과</label>
              <p className="mt-1 text-sm text-gray-900">{formatAuthResult(log.result)}</p>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-500">엔드포인트</label>
              <p className="mt-1 text-sm text-gray-900">{formatEndpoint(log.endpoint)}</p>
            </div>
          </div>
        </div>
      </ModalBody>
      <ModalFooter>
        <Button variant="outline" onClick={onClose}>
          닫기
        </Button>
      </ModalFooter>
    </Modal>
  );
};

export const AuthLogList: React.FC = () => {
  const [selectedLog, setSelectedLog] = useState<AuthLog | null>(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [localSearchCondition, setLocalSearchCondition] = useState<AuthSearchCondition>({
    page: 0,
    size: 20,
    sortBy: 'date',
    direction: 'desc',
  });

  const {
    allLogs,
    data,
    isLoading,
    isLoadingMore,
    error,
    stats,
    search,
    reset,
    loadMore
  } = useAuthLogs({
    initialCondition: {
      page: 0,
      size: 20,
      sortBy: 'date',
      direction: 'desc',
    },
    autoSearch: true
  });


  // 검색 조건 업데이트 (로컬 상태만 업데이트)
  const handleConditionChange = (updates: Partial<AuthSearchCondition>) => {
    setLocalSearchCondition(prev => ({ ...prev, ...updates }));
  };

  // 검색 실행
  const handleSearch = () => {
    search(localSearchCondition);
  };

  // 리셋
  const handleReset = () => {
    const defaultCondition = {
      page: 0,
      size: 20,
      sortBy: 'date' as const,
      direction: 'desc' as const,
    };
    setLocalSearchCondition(defaultCondition);
    reset();
  };

  // 상세 정보 보기
  const handleViewDetail = (log: AuthLog) => {
    setSelectedLog(log);
    setIsDetailModalOpen(true);
  };

  // 테이블 컬럼 정의
  const columns: TableColumn<AuthLog>[] = [
    {
      key: 'id',
      title: 'ID',
      width: '80px',
      render: (log) => (
        <span className="font-mono text-sm">{log.id}</span>
      ),
    },
    {
      key: 'date',
      title: '날짜/시간',
      render: (log) => (
        <div className="flex items-center gap-2">
          <Clock className="w-4 h-4 text-gray-400" />
          <span className="text-sm">{formatDateTime(log.date)}</span>
        </div>
      ),
    },
    {
      key: 'device',
      title: '디바이스',
      render: (log) => (
        <div className="flex items-center gap-2">
          <Smartphone className="w-4 h-4 text-gray-400" />
          <span className="text-sm">{formatDevice(log.device)}</span>
        </div>
      ),
    },
    {
      key: 'userId',
      title: '사용자 ID',
      render: (log) => (
        <div className="flex items-center gap-2">
          <User className="w-4 h-4 text-gray-400" />
          <span className="text-sm font-medium">{log.userId}</span>
        </div>
      ),
    },
    {
      key: 'result',
      title: '인증 결과',
      render: (log) => (
        <div className="flex items-center gap-2">
          <Activity className="w-4 h-4 text-gray-400" />
          <span className={cn(
            "px-2 py-1 rounded-full text-xs font-medium",
            log.result === 'success' && "bg-green-100 text-green-800",
            log.result === 'failure' && "bg-red-100 text-red-800",
            log.result === 'blocked' && "bg-yellow-100 text-yellow-800",
            log.result === 'expired' && "bg-gray-100 text-gray-800"
          )}>
            {formatAuthResult(log.result)}
          </span>
        </div>
      ),
    },
    {
      key: 'endpoint',
      title: '엔드포인트',
      render: (log) => (
        <span className="text-sm font-mono text-gray-600">
          {formatEndpoint(log.endpoint)}
        </span>
      ),
    },
    {
      key: 'actions',
      title: '작업',
      width: '100px',
      render: (log) => (
        <Button
          size="sm"
          variant="outline"
          onClick={() => handleViewDetail(log)}
        >
          상세 보기
        </Button>
      ),
    },
  ];

  // 로딩 중이거나 초기 상태일 때
  if (isLoading && !data) {
    return (
      <PageContainer
        title="인증 로그 조회"
        description="통합된 최적화 API를 사용한 인증 로그 검색 및 조회"
      >
        <div className="flex items-center justify-center py-12">
          <div className="flex items-center space-x-3">
            <div className="w-8 h-8 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin"></div>
            <span className="text-gray-600">데이터를 불러오는 중...</span>
          </div>
        </div>
      </PageContainer>
    );
  }

  return (
    <PageContainer
      title="인증 로그 조회"
      description="통합된 최적화 API를 사용한 인증 로그 검색 및 조회"
    >

      {/* 검색 필터 */}
      <SearchFilters
        condition={localSearchCondition}
        onConditionChange={handleConditionChange}
        onSearch={handleSearch}
        onReset={handleReset}
        isLoading={isLoading}
      />

      {/* 통계 정보 */}
      {stats && (
        <Card className="mb-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="text-center">
              <div className="text-2xl font-bold text-blue-600">
                {stats.currentResultCount.toLocaleString()}
              </div>
              <div className="text-sm text-gray-600">로드된 총 레코드</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-green-600">
                {stats.hasNext ? '로드 가능' : '마지막'}
              </div>
              <div className="text-sm text-gray-600">데이터 상태</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-purple-600">{stats.pageSize}</div>
              <div className="text-sm text-gray-600">요청당 로드 수</div>
            </div>
          </div>
        </Card>
      )}

      {/* 에러 메시지 */}
      {error && (
        <Card className="mb-6 bg-red-50 border-red-200">
          <div className="text-red-800">
            <strong>오류:</strong> {error}
          </div>
        </Card>
      )}

      {/* 테이블 */}
      <Card>
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-semibold">검색 결과</h3>
          <div className="flex gap-2">
            <Button
              variant="outline"
              onClick={handleSearch}
              disabled={isLoading}
              className="flex items-center gap-2"
            >
              <RefreshCw className={cn("w-4 h-4", isLoading && "animate-spin")} />
              새로고침
            </Button>
          </div>
        </div>

        <Table
          columns={columns}
          data={allLogs}
          loading={isLoading && allLogs.length === 0}
          emptyText="검색 결과가 없습니다."
        />

        {/* 더보기 버튼 */}
        {data && allLogs.length > 0 && (
          <div className="flex justify-center items-center mt-6 pt-4 border-t">
            <div className="text-center">
              <div className="text-sm text-gray-600 mb-3">
                현재 {allLogs.length.toLocaleString()}개 레코드가 로드되었습니다
              </div>
              {data.hasNext ? (
                <Button
                  onClick={loadMore}
                  disabled={isLoadingMore}
                  className="flex items-center gap-2"
                >
                  {isLoadingMore ? (
                    <>
                      <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                      더 불러오는 중...
                    </>
                  ) : (
                    '더보기'
                  )}
                </Button>
              ) : (
                <div className="text-sm text-gray-500">
                  모든 데이터를 불러왔습니다
                </div>
              )}
            </div>
          </div>
        )}
      </Card>

      {/* 상세 정보 모달 */}
      <AuthLogDetailModal
        log={selectedLog}
        isOpen={isDetailModalOpen}
        onClose={() => {
          setIsDetailModalOpen(false);
          setSelectedLog(null);
        }}
      />
    </PageContainer>
  );
};