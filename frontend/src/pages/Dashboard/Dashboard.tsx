import React, {useEffect, useState} from 'react';
import {Activity, AlertTriangle, Clock, FileSearch, Shield, Users} from 'lucide-react';
import {Card, PageContainer} from '@/components/layout';
import {Button} from '@/components/ui';
import {useAuthLogs} from '@/hooks/useAuthLogs';
import {useArchivingStats} from '@/hooks/useArchiving';
import {useDashboard} from '@/hooks/useDashboard';
import {DashboardService} from '@/services/dashboardService';
import {formatDateTime, formatFileSize, formatNumber} from '@/utils/format.utils';
import {cn} from '@/utils/cn.utils';

interface StatCardProps {
  title: string;
  value: string | number;
  icon: React.ReactNode;
  color?: 'blue' | 'green' | 'yellow' | 'red' | 'purple';
}

const StatCard: React.FC<StatCardProps> = ({
                                             title,
                                             value,
                                             icon,
                                             color = 'blue'
                                           }) => {
  const colorClasses = {
    blue: 'bg-blue-500 text-white',
    green: 'bg-green-500 text-white',
    yellow: 'bg-yellow-500 text-white',
    red: 'bg-red-500 text-white',
    purple: 'bg-purple-500 text-white'
  };

  return (
      <Card className="relative overflow-hidden">
        <div className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">{title}</p>
              <p className="text-2xl font-bold text-gray-900 mt-1">
                {typeof value === 'number' ? formatNumber(value) : value}
              </p>
            </div>
            <div className={cn('p-3 rounded-full', colorClasses[color])}>
              {icon}
            </div>
          </div>
        </div>
      </Card>
  );
};

const TodayStatsPanel: React.FC = () => {
  const [todayStats, setTodayStats] = useState<{
    totalCount: number;
    successCount: number;
    uniqueUserCount: number;
  }>({
    totalCount: 0,
    successCount: 0,
    uniqueUserCount: 0
  });

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const data = await DashboardService.getTodayStats();
        setTodayStats(data);
      } catch (error) {
        console.error('오늘 통계 조회 실패', error);
      }
    };
    fetchStats();
  }, []);

  const successRate =
      todayStats.totalCount > 0
          ? Math.round((todayStats.successCount / todayStats.totalCount) * 1000) / 10
          : 0;

  return (
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <StatCard
            title="오늘 인증 요청"
            value={todayStats.totalCount}
            icon={<Activity className="w-6 h-6" />}
            color="blue"
        />
        <StatCard
            title="성공률"
            value={`${successRate.toFixed(1)}%`}
            icon={<Shield className="w-6 h-6" />}
            color="green"
        />
        <StatCard
            title="인증한 유저 수"
            value={todayStats.uniqueUserCount}
            icon={<Users className="w-6 h-6" />}
            color="purple"
        />
      </div>
  );
};

export const Dashboard: React.FC = () => {
  const { logs: recentLogs, isLoading: logsLoading } = useAuthLogs({
    initialCondition: {
      page: 0,
      size: 5,
      sortBy: 'date',
      direction: 'desc'
    },
    autoSearch: true
  });
  const { stats, storageUsage, isLoading: statsLoading } = useArchivingStats();
  const { systemHealth } = useDashboard();

  return (
      <PageContainer
          title="대시보드"
          description="ReadFast 시스템 상태 및 주요 지표"
      >
        {/* 오늘 통계 */}
        <TodayStatsPanel />

        {/* 아카이빙 상태 */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
          <Card
              title="아카이빙 상태"
              subtitle="데이터 보관 및 스토리지 현황"
              action={
                <Button
                    variant="outline"
                    size="sm"
                    onClick={() => (window.location.href = '/archiving')}
                >
                  자세히 보기
                </Button>
              }
          >
            {statsLoading ? (
                <div className="animate-pulse">
                  <div className="h-4 bg-gray-200 rounded mb-3"></div>
                  <div className="h-4 bg-gray-200 rounded w-3/4 mb-3"></div>
                  <div className="h-4 bg-gray-200 rounded w-1/2"></div>
                </div>
            ) : (
                <div className="space-y-4">
                  {stats && (
                      <div className="grid grid-cols-2 gap-4">
                        <div>
                          <p className="text-sm text-gray-600">아카이브된 레코드</p>
                          <p className="text-xl font-semibold">
                            {formatNumber(stats.totalArchivedRecords)}
                          </p>
                        </div>
                        <div>
                          <p className="text-sm text-gray-600">압축률</p>
                          <p className="text-xl font-semibold">
                            {(stats.compressionRatio * 100).toFixed(1)}%
                          </p>
                        </div>
                      </div>
                  )}

                  {storageUsage && (
                      <div>
                        <div className="flex justify-between text-sm text-gray-600 mb-2">
                          <span>스토리지 사용량</span>
                          <span>{storageUsage.usagePercentage.toFixed(1)}%</span>
                        </div>
                        <div className="w-full bg-gray-200 rounded-full h-2">
                          <div
                              className={cn(
                                  'h-2 rounded-full transition-all duration-300',
                                  storageUsage.usagePercentage > 90
                                      ? 'bg-red-500'
                                      : storageUsage.usagePercentage > 75
                                          ? 'bg-yellow-500'
                                          : 'bg-green-500'
                              )}
                              style={{
                                width: `${Math.min(storageUsage.usagePercentage, 100)}%`
                              }}
                          />
                        </div>
                        <div className="flex justify-between text-xs text-gray-500 mt-1">
                          <span>{formatFileSize(storageUsage.usedSize)}</span>
                          <span>{formatFileSize(storageUsage.totalSize)}</span>
                        </div>
                      </div>
                  )}
                </div>
            )}
          </Card>

          {/* 시스템 알림 */}
          <Card
              title="시스템 알림"
              subtitle="주의가 필요한 이벤트"
              action={
                  systemHealth &&
                  (systemHealth.issues.length > 0 || systemHealth.warnings.length > 0) && (
                      <span className="bg-red-100 text-red-800 text-xs font-medium px-2.5 py-0.5 rounded-full">
                {systemHealth.issues.length + systemHealth.warnings.length}개
              </span>
                  )
              }
          >
            <div className="space-y-3">
              {systemHealth && (systemHealth.issues.length > 0 || systemHealth.warnings.length > 0) ? (
                  <>
                    {systemHealth.issues.map((issue, index) => (
                        <div key={`issue-${index}`} className="flex items-center space-x-3 p-3 bg-red-50 border border-red-200 rounded-lg">
                          <AlertTriangle className="w-5 h-5 text-red-600 flex-shrink-0" />
                          <div>
                            <p className="text-sm font-medium text-red-800">시스템 오류</p>
                            <p className="text-xs text-red-700">{issue}</p>
                          </div>
                        </div>
                    ))}
                    {systemHealth.warnings.map((warning, index) => (
                        <div key={`warning-${index}`} className="flex items-center space-x-3 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
                          <AlertTriangle className="w-5 h-5 text-yellow-600 flex-shrink-0" />
                          <div>
                            <p className="text-sm font-medium text-yellow-800">주의 필요</p>
                            <p className="text-xs text-yellow-700">{warning}</p>
                          </div>
                        </div>
                    ))}
                  </>
              ) : (
                  <div className="text-center py-4">
                    <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-3">
                      <Shield className="w-6 h-6 text-green-600" />
                    </div>
                    <p className="text-sm text-gray-600">모든 시스템이 정상 작동 중입니다.</p>
                    <div className="mt-4 flex items-center justify-center space-x-2 text-blue-600">
                      <Clock className="w-4 h-4" />
                      <p className="text-xs">내일 새벽 02:00 아카이빙 예정</p>
                    </div>
                  </div>
              )}
            </div>
          </Card>
        </div>

        {/* 최근 인증 로그 */}
        <Card
            title="최근 로그 활동"
            subtitle="최신 인증 로그 5건"
            action={
              <Button
                  variant="outline"
                  size="sm"
                  onClick={() => (window.location.href = '/auth-logs')}
              >
                전체 보기
              </Button>
            }
        >
          {logsLoading ? (
              <div className="animate-pulse space-y-3">
                {[...Array(5)].map((_, i) => (
                    <div key={i} className="flex items-center space-x-4">
                      <div className="w-10 h-10 bg-gray-200 rounded"></div>
                      <div className="flex-1">
                        <div className="h-4 bg-gray-200 rounded mb-2"></div>
                        <div className="h-3 bg-gray-200 rounded w-1/2"></div>
                      </div>
                      <div className="w-16 h-6 bg-gray-200 rounded"></div>
                    </div>
                ))}
              </div>
          ) : recentLogs.length > 0 ? (
              <div className="space-y-3">
                {recentLogs.map((log: any) => (
                    <div key={log.id} className="flex items-center space-x-4 p-3 hover:bg-gray-50 rounded-lg transition-colors">
                      <div className={cn(
                          'w-10 h-10 rounded-full flex items-center justify-center',
                          log.result === 'success' ? 'bg-green-100 text-green-600' :
                              log.result === 'failure' ? 'bg-red-100 text-red-600' :
                                  'bg-yellow-100 text-yellow-600'
                      )}>
                        {log.result === 'success' ? '✓' : log.result === 'failure' ? '✗' : '!'}
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-gray-900 truncate">
                          {log.userId} • {log.device}
                        </p>
                        <p className="text-xs text-gray-500 truncate">
                          {log.endpoint}
                        </p>
                      </div>
                      <div className="text-right">
                        <p className="text-xs text-gray-500">
                          {formatDateTime(log.date)}
                        </p>
                      </div>
                    </div>
                ))}
              </div>
          ) : (
              <div className="text-center py-6">
                <FileSearch className="w-12 h-12 text-gray-400 mx-auto mb-3" />
                <p className="text-sm text-gray-600">최근 로그가 없습니다.</p>
              </div>
          )}
        </Card>
      </PageContainer>
  );
};
