import React, { useState } from 'react';
import { 
  Play,
  Pause,
  Database,
  Clock,
  CheckCircle,
  AlertTriangle,
  Calendar
} from 'lucide-react';
import { PageContainer, Card } from '@/components/layout';
import { Button, Input } from '@/components/ui';
import { useArchiving, useArchivingStats } from '@/hooks/useArchiving';
import { 
  formatNumber, 
  formatFileSize, 
  formatDuration, 
  formatDateTime,
  formatPercentage
} from '@/utils/format.utils';
import { cn } from '@/utils/cn.utils';

export const ArchivingDashboard: React.FC = () => {
  const [showExecuteConfirm, setShowExecuteConfirm] = useState(false);
  const [showDateExecuteModal, setShowDateExecuteModal] = useState(false);
  const [selectedDate, setSelectedDate] = useState(() => {
    const today = new Date();
    today.setDate(today.getDate() - 1); // 어제 날짜를 기본값으로
    return today.toISOString().split('T')[0];
  });
  
  const {
    status,
    isLoadingStatus,
    statusError,
    executeArchiving,
    executeArchivingByDate,
    pauseArchiving,
    resumeArchiving,
    refreshStatus,
    isExecuting,
    executionError,
    lastExecutionResult,
  } = useArchiving();

  const {
    stats,
    storageUsage,
    isLoading: statsLoading,
    error: statsError,
    refresh: refreshStats,
  } = useArchivingStats();

  // 수동 아카이빙 실행
  const handleExecuteArchiving = async () => {
    setShowExecuteConfirm(false);
    const result = await executeArchiving();
    
    if (result?.success) {
      // 통계 새로고침
      setTimeout(() => {
        refreshStats();
      }, 2000);
    }
  };

  // 날짜 지정 아카이빙 실행
  const handleExecuteArchivingByDate = async () => {
    setShowDateExecuteModal(false);
    const result = await executeArchivingByDate(selectedDate);
    
    if (result?.success) {
      // 통계 새로고침
      setTimeout(() => {
        refreshStats();
      }, 2000);
    }
  };

  // 아카이빙 일시 중지/재개
  const handleToggleArchiving = async () => {
    if (status?.isRunning) {
      await pauseArchiving();
    } else {
      await resumeArchiving();
    }
  };

  return (
    <PageContainer
      title="데이터 아카이빙"
      description="오래된 데이터를 아카이브하고 스토리지를 관리합니다"
      breadcrumbs={[
        { label: '홈', href: '/' },
        { label: '데이터 아카이빙' },
      ]}
      action={
        <div className="flex space-x-2">
          <Button
            variant="outline"
            onClick={refreshStatus}
            loading={isLoadingStatus}
          >
            상태 새로고침
          </Button>
          <Button
            variant="outline"
            onClick={() => setShowDateExecuteModal(true)}
            loading={isExecuting}
            disabled={status?.isRunning}
            leftIcon={<Calendar className="w-4 h-4" />}
          >
            날짜 지정 실행
          </Button>
          <Button
            onClick={() => setShowExecuteConfirm(true)}
            loading={isExecuting}
            disabled={status?.isRunning}
            leftIcon={<Play className="w-4 h-4" />}
          >
            수동 실행
          </Button>
        </div>
      }
    >
      {/* 현재 상태 */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <Card>
          <div className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">아카이빙 상태</p>
                <p className="text-lg font-semibold mt-1">
                  {isLoadingStatus ? (
                    <span className="text-gray-400">로딩 중...</span>
                  ) : statusError ? (
                    <span className="text-red-600">오류</span>
                  ) : status?.isRunning ? (
                    <span className="text-green-600">실행 중</span>
                  ) : (
                    <span className="text-gray-600">대기</span>
                  )}
                </p>
              </div>
              <div className={cn(
                'p-3 rounded-full',
                status?.isRunning ? 'bg-green-100 text-green-600' : 'bg-gray-100 text-gray-400'
              )}>
                {status?.isRunning ? (
                  <Play className="w-6 h-6" />
                ) : (
                  <Pause className="w-6 h-6" />
                )}
              </div>
            </div>
          </div>
        </Card>

        <Card>
          <div className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">대기열 크기</p>
                <p className="text-lg font-semibold mt-1">
                  {status?.queueSize ?? 0}건
                </p>
              </div>
              <div className="p-3 rounded-full bg-blue-100 text-blue-600">
                <Database className="w-6 h-6" />
              </div>
            </div>
          </div>
        </Card>

        <Card>
          <div className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">다음 예정 시간</p>
                <p className="text-sm text-gray-900 mt-1">
                  {status?.nextScheduledTime 
                    ? formatDateTime(status.nextScheduledTime)
                    : '설정되지 않음'
                  }
                </p>
              </div>
              <div className="p-3 rounded-full bg-purple-100 text-purple-600">
                <Clock className="w-6 h-6" />
              </div>
            </div>
          </div>
        </Card>
      </div>

      {/* 최근 실행 결과 */}
      {(lastExecutionResult || status?.lastExecution) && (
        <Card title="최근 실행 결과" className="mb-8">
          {executionError && (
            <div className="p-4 mb-4 bg-red-50 border border-red-200 rounded-md">
              <p className="text-sm text-red-600">{executionError}</p>
            </div>
          )}
          
          {lastExecutionResult && (
            <div className="p-6">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="flex items-center space-x-3">
                  {lastExecutionResult.success ? (
                    <CheckCircle className="w-8 h-8 text-green-600" />
                  ) : (
                    <AlertTriangle className="w-8 h-8 text-red-600" />
                  )}
                  <div>
                    <p className="text-sm text-gray-600">실행 결과</p>
                    <p className="font-semibold">
                      {lastExecutionResult.success ? '성공' : '실패'}
                    </p>
                  </div>
                </div>

                <div>
                  <p className="text-sm text-gray-600">처리된 레코드</p>
                  <p className="text-xl font-semibold">
                    {formatNumber(lastExecutionResult.processedRecords)}건
                  </p>
                </div>

                <div>
                  <p className="text-sm text-gray-600">실행 시간</p>
                  <p className="text-xl font-semibold">
                    {formatDuration(lastExecutionResult.durationMillis)}
                  </p>
                </div>
              </div>
              
              {lastExecutionResult.message && (
                <div className="mt-4 p-3 bg-gray-50 rounded">
                  <p className="text-sm text-gray-700">
                    {lastExecutionResult.message}
                  </p>
                </div>
              )}
            </div>
          )}
        </Card>
      )}

      {/* 통계 및 스토리지 현황 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        {/* 아카이빙 통계 */}
        <Card title="아카이빙 통계" subtitle="전체 아카이빙 현황">
          {statsLoading ? (
            <div className="p-6 animate-pulse">
              <div className="space-y-4">
                {[...Array(4)].map((_, i) => (
                  <div key={i} className="flex justify-between">
                    <div className="h-4 bg-gray-200 rounded w-1/3"></div>
                    <div className="h-4 bg-gray-200 rounded w-1/4"></div>
                  </div>
                ))}
              </div>
            </div>
          ) : statsError ? (
            <div className="p-6 text-center text-red-600">
              <p>통계를 불러올 수 없습니다.</p>
            </div>
          ) : stats ? (
            <div className="p-6 space-y-4">
              <div className="flex justify-between">
                <span className="text-gray-600">아카이브된 레코드</span>
                <span className="font-semibold">
                  {formatNumber(stats.totalArchivedRecords)}
                </span>
              </div>
              
              <div className="flex justify-between">
                <span className="text-gray-600">아카이브 파일 수</span>
                <span className="font-semibold">
                  {formatNumber(stats.totalArchiveFiles)}
                </span>
              </div>
              
              <div className="flex justify-between">
                <span className="text-gray-600">전체 크기</span>
                <span className="font-semibold">
                  {formatFileSize(stats.totalArchivedSize)}
                </span>
              </div>
              
              <div className="flex justify-between">
                <span className="text-gray-600">평균 압축률</span>
                <span className="font-semibold">
                  {formatPercentage(stats.compressionRatio)}
                </span>
              </div>
              
              <div className="flex justify-between">
                <span className="text-gray-600">평균 실행 시간</span>
                <span className="font-semibold">
                  {formatDuration(stats.averageExecutionTime)}
                </span>
              </div>
              
              <div className="flex justify-between">
                <span className="text-gray-600">실패율</span>
                <span className={cn(
                  'font-semibold',
                  stats.failureRate > 10 ? 'text-red-600' :
                  stats.failureRate > 5 ? 'text-yellow-600' : 'text-green-600'
                )}>
                  {formatPercentage(stats.failureRate / 100)}
                </span>
              </div>
            </div>
          ) : (
            <div className="p-6 text-center text-gray-500">
              <p>통계 데이터가 없습니다.</p>
            </div>
          )}
        </Card>

        {/* 스토리지 현황 */}
        <Card title="스토리지 현황" subtitle="디스크 사용량 및 상태">
          {statsLoading ? (
            <div className="p-6 animate-pulse">
              <div className="space-y-4">
                <div className="h-4 bg-gray-200 rounded"></div>
                <div className="h-2 bg-gray-200 rounded"></div>
                <div className="flex justify-between">
                  <div className="h-3 bg-gray-200 rounded w-1/4"></div>
                  <div className="h-3 bg-gray-200 rounded w-1/4"></div>
                </div>
              </div>
            </div>
          ) : storageUsage ? (
            <div className="p-6">
              <div className="mb-4">
                <div className="flex justify-between text-sm text-gray-600 mb-2">
                  <span>사용량</span>
                  <span>{formatPercentage(storageUsage.usagePercentage / 100)}</span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-3">
                  <div
                    className={cn(
                      'h-3 rounded-full transition-all duration-300',
                      storageUsage.usagePercentage > 90 ? 'bg-red-500' :
                      storageUsage.usagePercentage > 75 ? 'bg-yellow-500' : 'bg-green-500'
                    )}
                    style={{ width: `${Math.min(storageUsage.usagePercentage, 100)}%` }}
                  />
                </div>
                <div className="flex justify-between text-xs text-gray-500 mt-1">
                  <span>{formatFileSize(storageUsage.usedSize)}</span>
                  <span>{formatFileSize(storageUsage.totalSize)}</span>
                </div>
              </div>

              <div className="space-y-3">
                <div className="flex justify-between">
                  <span className="text-gray-600">사용 가능한 용량</span>
                  <span className="font-semibold">
                    {formatFileSize(storageUsage.availableSize)}
                  </span>
                </div>
                
                <div className="flex justify-between">
                  <span className="text-gray-600">파일 개수</span>
                  <span className="font-semibold">
                    {formatNumber(storageUsage.fileCount)}개
                  </span>
                </div>
                
                <div className="flex justify-between">
                  <span className="text-gray-600">스토리지 타입</span>
                  <span className="font-semibold">
                    {storageUsage.storageType}
                  </span>
                </div>
                
                <div className="flex justify-between">
                  <span className="text-gray-600">마지막 업데이트</span>
                  <span className="text-sm text-gray-500">
                    {formatDateTime(storageUsage.lastUpdated)}
                  </span>
                </div>
              </div>

              {storageUsage.usagePercentage > 90 && (
                <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded">
                  <p className="text-sm text-red-800">
                    ⚠️ 스토리지 용량이 부족합니다. 공간을 확보하거나 용량을 늘려주세요.
                  </p>
                </div>
              )}
            </div>
          ) : (
            <div className="p-6 text-center text-gray-500">
              <p>스토리지 정보를 가져올 수 없습니다.</p>
            </div>
          )}
        </Card>
      </div>

      {/* 아카이빙 제어 */}
      <Card title="아카이빙 제어" subtitle="아카이빙 프로세스를 관리합니다">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm text-gray-600">
              아카이빙 프로세스를 수동으로 제어할 수 있습니다.
            </p>
          </div>
          
          <div className="flex space-x-3">
            <Button
              variant={status?.isRunning ? "danger" : "success"}
              onClick={handleToggleArchiving}
              disabled={isLoadingStatus}
              leftIcon={status?.isRunning ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4" />}
            >
              {status?.isRunning ? '일시 중지' : '재개'}
            </Button>
          </div>
        </div>
      </Card>

      {/* 수동 실행 확인 모달 */}
      {showExecuteConfirm && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md mx-4">
            <h3 className="text-lg font-semibold mb-4">아카이빙 실행 확인</h3>
            <p className="text-gray-600 mb-6">
              수동으로 아카이빙을 실행하시겠습니까? 
              이 작업은 시간이 오래 걸릴 수 있습니다.
            </p>
            <div className="flex justify-end space-x-3">
              <Button
                variant="outline"
                onClick={() => setShowExecuteConfirm(false)}
              >
                취소
              </Button>
              <Button
                onClick={handleExecuteArchiving}
                loading={isExecuting}
              >
                실행
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* 날짜 지정 실행 모달 */}
      {showDateExecuteModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md mx-4">
            <h3 className="text-lg font-semibold mb-4">날짜 지정 아카이빙</h3>
            <div className="mb-4">
              <Input
                label="아카이빙 대상 날짜"
                type="date"
                value={selectedDate}
                onChange={(e) => setSelectedDate(e.target.value)}
                helperText="지정한 날짜의 데이터를 아카이빙합니다"
              />
            </div>
            <p className="text-gray-600 mb-6">
              {selectedDate}의 데이터를 아카이빙하시겠습니까?
            </p>
            <div className="flex justify-end space-x-3">
              <Button
                variant="outline"
                onClick={() => setShowDateExecuteModal(false)}
              >
                취소
              </Button>
              <Button
                onClick={handleExecuteArchivingByDate}
                loading={isExecuting}
                leftIcon={<Calendar className="w-4 h-4" />}
              >
                실행
              </Button>
            </div>
          </div>
        </div>
      )}
    </PageContainer>
  );
};