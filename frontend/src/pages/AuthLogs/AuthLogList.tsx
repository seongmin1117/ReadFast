import React, { useState } from 'react';
import { 
  Search,
  Download,
  RefreshCw,
  Smartphone,
  User,
  Activity,
  Zap,
  Clock,
  BarChart3
} from 'lucide-react';
import { PageContainer, Card } from '@/components/layout';
import { 
  Button, 
  Input, 
  Table, 
  DateRangePicker,
  Modal,
  ModalBody,
  ModalFooter
} from '@/components/ui';
import { useAuthLogs } from '@/hooks/useAuthLogs';
import { 
  formatDateTime, 
  formatDevice, 
  formatAuthResult,
  formatEndpoint 
} from '@/utils/format.utils';
import { cn } from '@/utils/cn.utils';
import type { AuthLog, AuthSearchCondition, TableColumn, ApiVersion, ApiPerformanceMetrics } from '@/types';

// 버전 선택 컴포넌트
const VersionSelector: React.FC<{
  currentVersion: ApiVersion;
  onVersionChange: (version: ApiVersion) => void;
  performanceMetrics: ApiPerformanceMetrics | null;
}> = ({ currentVersion, onVersionChange, performanceMetrics }) => {
  const versions: { value: ApiVersion; label: string; description: string }[] = [
    { value: 'v1', label: 'V1 (오프셋)', description: '전통적인 오프셋 기반 페이지네이션' },
    { value: 'v2', label: 'V2 (커서)', description: '성능 개선된 커서 기반 페이지네이션' },
    { value: 'v3', label: 'V3 (통합)', description: 'DB + 스토리지 통합 조회' },
  ];

  return (
    <Card title="API 버전 선택" className="mb-6">
      <div className="space-y-4">
        {/* 버전 선택 라디오 버튼 */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {versions.map((version) => (
            <label
              key={version.value}
              className={cn(
                'relative flex cursor-pointer rounded-lg border p-4 transition-colors',
                currentVersion === version.value
                  ? 'border-blue-500 bg-blue-50 text-blue-900'
                  : 'border-gray-300 bg-white text-gray-900 hover:bg-gray-50'
              )}
            >
              <input
                type="radio"
                name="version"
                value={version.value}
                checked={currentVersion === version.value}
                onChange={() => onVersionChange(version.value)}
                className="sr-only"
              />
              <div className="flex flex-col">
                <span className="block text-sm font-medium">{version.label}</span>
                <span className="block text-xs text-gray-500 mt-1">{version.description}</span>
              </div>
              {currentVersion === version.value && (
                <div className="absolute top-2 right-2">
                  <Zap className="h-4 w-4 text-blue-500" />
                </div>
              )}
            </label>
          ))}
        </div>

        {/* 성능 메트릭 표시 */}
        {performanceMetrics && (
          <div className="flex items-center space-x-6 p-3 bg-gray-50 rounded-md">
            <div className="flex items-center space-x-2">
              <Clock className="h-4 w-4 text-gray-500" />
              <span className="text-sm text-gray-600">응답시간:</span>
              <span className="text-sm font-medium text-gray-900">
                {performanceMetrics.responseTime}ms
              </span>
            </div>
            <div className="flex items-center space-x-2">
              <BarChart3 className="h-4 w-4 text-gray-500" />
              <span className="text-sm text-gray-600">총 레코드:</span>
              <span className="text-sm font-medium text-gray-900">
                {performanceMetrics.totalRecords.toLocaleString()}건
              </span>
            </div>
            <div className="flex items-center space-x-2">
              <span className="text-sm text-gray-600">측정시간:</span>
              <span className="text-sm font-medium text-gray-900">
                {new Date(performanceMetrics.timestamp).toLocaleTimeString()}
              </span>
            </div>
          </div>
        )}
      </div>
    </Card>
  );
};

// 검색 필터 컴포넌트
const SearchFilters: React.FC<{
  condition: AuthSearchCondition;
  onChange: (updates: Partial<AuthSearchCondition>) => void;
  onSearch: () => void;
  onReset: () => void;
  isLoading: boolean;
}> = ({ condition, onChange, onSearch, onReset, isLoading }) => {
  return (
    <Card title="검색 필터" className="mb-6">
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-4">
        {/* 날짜 범위 */}
        <DateRangePicker
          label="기간"
          startDate={condition.startDate}
          endDate={condition.endDate}
          onStartDateChange={(date) => onChange({ startDate: date })}
          onEndDateChange={(date) => onChange({ endDate: date })}
          showPresets
        />

        {/* 사용자 ID */}
        <Input
          label="사용자 ID"
          placeholder="사용자 ID 검색"
          value={condition.userId || ''}
          onChange={(e) => onChange({ userId: e.target.value })}
          leftIcon={<User className="w-4 h-4" />}
        />

        {/* 디바이스 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            디바이스
          </label>
          <select
            value={condition.device || ''}
            onChange={(e) => onChange({ device: e.target.value })}
            className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          >
            <option value="">전체</option>
            <option value="mobile">모바일</option>
            <option value="desktop">데스크톱</option>
            <option value="tablet">태블릿</option>
            <option value="web">웹</option>
          </select>
        </div>

        {/* 인증 결과 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            인증 결과
          </label>
          <select
            value={condition.result || ''}
            onChange={(e) => onChange({ result: e.target.value })}
            className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          >
            <option value="">전체</option>
            <option value="success">성공</option>
            <option value="failure">실패</option>
            <option value="blocked">차단</option>
            <option value="expired">만료</option>
          </select>
        </div>

        {/* 엔드포인트 */}
        <Input
          label="엔드포인트"
          placeholder="API 엔드포인트 검색"
          value={condition.endpoint || ''}
          onChange={(e) => onChange({ endpoint: e.target.value })}
          leftIcon={<Activity className="w-4 h-4" />}
        />

        {/* 정렬 옵션 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            정렬
          </label>
          <div className="flex space-x-2">
            <select
              value={condition.sortBy || 'date'}
              onChange={(e) => onChange({ sortBy: e.target.value as AuthSearchCondition['sortBy'] })}
              className="flex-1 rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="date">날짜</option>
              <option value="userId">사용자 ID</option>
              <option value="device">디바이스</option>
              <option value="result">결과</option>
              <option value="endpoint">엔드포인트</option>
            </select>
            <select
              value={condition.direction || 'desc'}
              onChange={(e) => onChange({ direction: e.target.value as AuthSearchCondition['direction'] })}
              className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="desc">최신순</option>
              <option value="asc">과거순</option>
            </select>
          </div>
        </div>
      </div>

      <div className="flex flex-wrap gap-2">
        <Button 
          onClick={onSearch}
          loading={isLoading}
          leftIcon={<Search className="w-4 h-4" />}
        >
          검색
        </Button>
        <Button 
          variant="outline"
          onClick={onReset}
          leftIcon={<RefreshCw className="w-4 h-4" />}
        >
          초기화
        </Button>
      </div>
    </Card>
  );
};

// 로그 상세보기 모달
const LogDetailModal: React.FC<{
  log: AuthLog | null;
  isOpen: boolean;
  onClose: () => void;
}> = ({ log, isOpen, onClose }) => {
  if (!log) return null;

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="인증 로그 상세 정보" size="lg">
      <ModalBody>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              로그 ID
            </label>
            <p className="text-sm text-gray-900">{log.id}</p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              일시
            </label>
            <p className="text-sm text-gray-900">{formatDateTime(log.date)}</p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              사용자 ID
            </label>
            <p className="text-sm text-gray-900">{log.userId}</p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              디바이스
            </label>
            <p className="text-sm text-gray-900">{formatDevice(log.device)}</p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              인증 결과
            </label>
            <span className={cn(
              'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
              log.result === 'success' ? 'bg-green-100 text-green-800' :
              log.result === 'failure' ? 'bg-red-100 text-red-800' :
              'bg-yellow-100 text-yellow-800'
            )}>
              {formatAuthResult(log.result)}
            </span>
          </div>

          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              엔드포인트
            </label>
            <p className="text-sm text-gray-900 bg-gray-50 px-3 py-2 rounded">
              {log.endpoint}
            </p>
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

  const {
    logs,
    data,
    isLoading,
    error,
    searchCondition,
    stats,
    currentVersion,
    setVersion,
    performanceMetrics,
    allPerformanceMetrics,
    search,
    searchAllVersions,
    refresh,
    updateCondition,
    exportLogs,
    clearPerformanceMetrics,
  } = useAuthLogs({
    initialCondition: {
      page: 0,
      size: 20,
      sortBy: 'date',
      direction: 'desc',
    },
    defaultVersion: 'v3',
  });

  // 검색 실행
  const handleSearch = () => {
    search();
  };

  // 모든 버전에서 성능 비교 검색
  const handleCompareVersions = () => {
    searchAllVersions();
  };

  // 버전 변경 시 자동 검색
  const handleVersionChange = (version: ApiVersion) => {
    setVersion(version);
    search(undefined, version);
  };

  // 검색 조건 초기화
  const handleReset = () => {
    updateCondition({
      startDate: undefined,
      endDate: undefined,
      device: undefined,
      userId: undefined,
      result: undefined,
      endpoint: undefined,
      sortBy: 'date',
      direction: 'desc',
    });
  };

  // 페이지네이션
  const handlePageChange = (page: number) => {
    search({ page });
  };

  const handlePageSizeChange = (size: number) => {
    search({ size, page: 0 });
  };

  // 로그 상세보기
  const handleLogClick = (log: AuthLog) => {
    setSelectedLog(log);
    setIsDetailModalOpen(true);
  };

  // 내보내기
  const handleExport = async (format: 'csv' | 'excel' | 'json' = 'csv') => {
    await exportLogs(format, currentVersion);
  };

  // 테이블 컬럼 정의
  const columns: TableColumn<AuthLog>[] = [
    {
      key: 'id',
      title: 'ID',
      width: 80,
      render: (value) => (
        <span className="font-mono text-xs">{value}</span>
      ),
    },
    {
      key: 'date',
      title: '일시',
      width: 160,
      render: (value) => formatDateTime(value),
    },
    {
      key: 'userId',
      title: '사용자 ID',
      width: 120,
      render: (value) => (
        <span className="font-medium">{value}</span>
      ),
    },
    {
      key: 'device',
      title: '디바이스',
      width: 100,
      render: (value) => (
        <span className="inline-flex items-center space-x-1">
          <Smartphone className="w-3 h-3" />
          <span>{formatDevice(value)}</span>
        </span>
      ),
    },
    {
      key: 'result',
      title: '결과',
      width: 100,
      render: (value) => (
        <span className={cn(
          'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
          value === 'success' ? 'bg-green-100 text-green-800' :
          value === 'failure' ? 'bg-red-100 text-red-800' :
          'bg-yellow-100 text-yellow-800'
        )}>
          {formatAuthResult(value)}
        </span>
      ),
    },
    {
      key: 'endpoint',
      title: '엔드포인트',
      render: (value) => (
        <span className="font-mono text-xs text-gray-600">
          {formatEndpoint(value)}
        </span>
      ),
    },
  ];

  return (
    <PageContainer
      title="인증 로그"
      description="시스템의 모든 인증 로그를 조회하고 관리합니다"
      breadcrumbs={[
        { label: '홈', href: '/' },
        { label: '인증 로그' },
      ]}
      action={
        <div className="flex space-x-2">
          <Button
            variant="outline"
            onClick={handleCompareVersions}
            loading={isLoading}
            leftIcon={<BarChart3 className="w-4 h-4" />}
          >
            성능 비교
          </Button>
          <Button
            variant="outline"
            onClick={clearPerformanceMetrics}
            leftIcon={<Clock className="w-4 h-4" />}
          >
            메트릭 초기화
          </Button>
          <Button
            variant="outline"
            onClick={refresh}
            loading={isLoading}
            leftIcon={<RefreshCw className="w-4 h-4" />}
          >
            새로고침
          </Button>
          <Button
            variant="outline"
            onClick={() => handleExport('csv')}
            leftIcon={<Download className="w-4 h-4" />}
          >
            내보내기
          </Button>
        </div>
      }
    >
      {/* 버전 선택 */}
      <VersionSelector
        currentVersion={currentVersion}
        onVersionChange={handleVersionChange}
        performanceMetrics={performanceMetrics}
      />

      {/* 검색 필터 */}
      <SearchFilters
        condition={searchCondition}
        onChange={updateCondition}
        onSearch={handleSearch}
        onReset={handleReset}
        isLoading={isLoading}
      />

      {/* 검색 결과 통계 */}
      {stats && (
        <Card className="mb-6">
          <div className="flex items-center justify-between p-4">
            <div className="flex items-center space-x-6">
              <div>
                <p className="text-sm text-gray-600">검색 결과</p>
                <p className="text-lg font-semibold">
                  {data?.totalElements.toLocaleString() || 0}건
                </p>
              </div>
              <div>
                <p className="text-sm text-gray-600">검색 시간 ({currentVersion.toUpperCase()})</p>
                <p className="text-lg font-semibold">{stats.searchDuration}ms</p>
              </div>
              {data && (
                <div>
                  <p className="text-sm text-gray-600">현재 페이지</p>
                  <p className="text-lg font-semibold">
                    {data.page + 1} / {data.totalPages}
                  </p>
                </div>
              )}
              {stats.fromCache && (
                <div className="px-2 py-1 bg-green-100 text-green-800 text-xs rounded">
                  캐시됨
                </div>
              )}
            </div>
          </div>
        </Card>
      )}

      {/* 성능 비교 대시보드 */}
      {allPerformanceMetrics.length > 0 && (
        <Card title="성능 비교 결과" className="mb-6">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    API 버전
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    응답 시간
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    총 레코드
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    측정 시간
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    성능 등급
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {allPerformanceMetrics
                  .sort((a, b) => a.responseTime - b.responseTime)
                  .map((metric, index) => {
                    const isCurrentVersion = metric.version === currentVersion;
                    const isFastest = index === 0;
                    return (
                      <tr 
                        key={metric.version}
                        className={isCurrentVersion ? 'bg-blue-50' : ''}
                      >
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="flex items-center">
                            <span className={cn(
                              "text-sm font-medium",
                              isCurrentVersion ? "text-blue-900" : "text-gray-900"
                            )}>
                              {metric.version.toUpperCase()}
                            </span>
                            {isCurrentVersion && (
                              <span className="ml-2 px-2 py-1 text-xs bg-blue-100 text-blue-800 rounded">
                                현재
                              </span>
                            )}
                            {isFastest && (
                              <span className="ml-2 px-2 py-1 text-xs bg-green-100 text-green-800 rounded">
                                최고 성능
                              </span>
                            )}
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                          <div className="flex items-center">
                            <Clock className="h-4 w-4 text-gray-400 mr-2" />
                            {metric.responseTime}ms
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                          {metric.totalRecords.toLocaleString()}건
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                          {new Date(metric.timestamp).toLocaleTimeString()}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span className={cn(
                            "px-2 py-1 text-xs rounded-full",
                            metric.responseTime < 100 ? "bg-green-100 text-green-800" :
                            metric.responseTime < 500 ? "bg-yellow-100 text-yellow-800" :
                            "bg-red-100 text-red-800"
                          )}>
                            {metric.responseTime < 100 ? "우수" :
                             metric.responseTime < 500 ? "보통" : "개선 필요"}
                          </span>
                        </td>
                      </tr>
                    );
                  })}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* 로그 테이블 */}
      <Card>
        {error && (
          <div className="p-4 mb-4 bg-red-50 border border-red-200 rounded-md">
            <p className="text-sm text-red-600">{error}</p>
          </div>
        )}

        <Table
          columns={columns}
          data={logs}
          loading={isLoading}
          onRowClick={handleLogClick}
          emptyText="검색 조건에 맞는 로그가 없습니다."
        />

        {/* 페이지네이션 */}
        {data && data.totalElements > 0 && (
          <div className="px-6 py-4 border-t border-gray-200">
            <div className="flex items-center justify-between">
              <div className="text-sm text-gray-700">
                총 {data.totalElements.toLocaleString()}건 중{' '}
                {((data.page * data.size) + 1).toLocaleString()}-
                {Math.min((data.page + 1) * data.size, data.totalElements).toLocaleString()}건
              </div>

              <div className="flex items-center space-x-2">
                <select
                  value={searchCondition.size || 20}
                  onChange={(e) => handlePageSizeChange(Number(e.target.value))}
                  className="rounded border border-gray-300 px-2 py-1 text-sm"
                >
                  <option value={10}>10개씩</option>
                  <option value={20}>20개씩</option>
                  <option value={50}>50개씩</option>
                  <option value={100}>100개씩</option>
                </select>

                <div className="flex space-x-1">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handlePageChange(0)}
                    disabled={!data.page}
                  >
                    처음
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handlePageChange(data.page - 1)}
                    disabled={!data.page}
                  >
                    이전
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handlePageChange(data.page + 1)}
                    disabled={!data.hasNext}
                  >
                    다음
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handlePageChange(data.totalPages - 1)}
                    disabled={!data.hasNext}
                  >
                    마지막
                  </Button>
                </div>
              </div>
            </div>
          </div>
        )}
      </Card>

      {/* 로그 상세보기 모달 */}
      <LogDetailModal
        log={selectedLog}
        isOpen={isDetailModalOpen}
        onClose={() => setIsDetailModalOpen(false)}
      />
    </PageContainer>
  );
};