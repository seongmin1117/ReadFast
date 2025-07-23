import React, { useState, useEffect } from 'react';
import { Save, RotateCcw } from 'lucide-react';
import { PageContainer, Card } from '@/components/layout';
import { Button, Input } from '@/components/ui';
import { PolicyService } from '@/services/policyService';
import type { DataRetentionPolicy, UpdatePolicyRequest } from '@/types/policy.types';

export const PolicySettings: React.FC = () => {
  const [policy, setPolicy] = useState<DataRetentionPolicy | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // 폼 데이터
  const [formData, setFormData] = useState<UpdatePolicyRequest>({});

  // 정책 로드
  const loadPolicy = async () => {
    try {
      setIsLoading(true);
      setError(null);
      
      const data = await PolicyService.getDefaultPolicy();
      setPolicy(data);
      
      // 폼 데이터 초기화
      setFormData({
        name: data.name,
        dbRetentionDays: data.dbRetentionDays,
        totalRetentionDays: data.totalRetentionDays,
        batchSize: data.batchSize,
        archiveBasePath: data.archiveBasePath,
        archiveFileFormat: data.archiveFileFormat,
        enableArchiving: data.enableArchiving,
        enableDataDeletion: data.enableDataDeletion,
        cronExpression: data.cronExpression,
      });
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '정책 로드 중 오류가 발생했습니다.';
      setError(errorMessage);
      console.error('Failed to load policy:', err);
    } finally {
      setIsLoading(false);
    }
  };

  // 정책 저장
  const savePolicy = async () => {
    if (!policy) return;

    try {
      setSaving(true);
      setError(null);
      setSuccess(null);

      // 정책 검증
      const validation = await PolicyService.validatePolicy(formData);
      if (!validation.isValid) {
        setError(validation.errors.join(', '));
        return;
      }

      // 정책 업데이트
      const updatedPolicy = await PolicyService.updateDefaultPolicy(formData);
      setPolicy(updatedPolicy);
      setSuccess('정책이 성공적으로 저장되었습니다.');
      
      // 성공 메시지 자동 제거
      setTimeout(() => setSuccess(null), 5000);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '정책 저장 중 오류가 발생했습니다.';
      setError(errorMessage);
      console.error('Failed to save policy:', err);
    } finally {
      setSaving(false);
    }
  };

  // 폼 리셋
  const resetForm = () => {
    if (policy) {
      setFormData({
        name: policy.name,
        dbRetentionDays: policy.dbRetentionDays,
        totalRetentionDays: policy.totalRetentionDays,
        batchSize: policy.batchSize,
        archiveBasePath: policy.archiveBasePath,
        archiveFileFormat: policy.archiveFileFormat,
        enableArchiving: policy.enableArchiving,
        enableDataDeletion: policy.enableDataDeletion,
        cronExpression: policy.cronExpression,
      });
    }
    setError(null);
    setSuccess(null);
  };

  // 폼 데이터 업데이트
  const updateFormData = (updates: Partial<UpdatePolicyRequest>) => {
    setFormData(prev => ({ ...prev, ...updates }));
    setError(null);
    setSuccess(null);
  };

  // 다음 실행 시간 계산
  const nextExecutionTime = formData.cronExpression 
    ? PolicyService.getNextExecutionTime(formData.cronExpression)
    : null;

  // 초기 로드
  useEffect(() => {
    loadPolicy();
  }, []);

  if (isLoading) {
    return (
      <PageContainer title="설정" description="시스템 설정을 관리합니다">
        <Card>
          <div className="p-8 text-center">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4"></div>
            <p className="text-gray-600">설정을 불러오는 중...</p>
          </div>
        </Card>
      </PageContainer>
    );
  }

  return (
    <PageContainer
      title="설정"
      description="데이터 보존 정책 및 시스템 설정을 관리합니다"
      breadcrumbs={[
        { label: '홈', href: '/' },
        { label: '설정' },
      ]}
      action={
        <div className="flex space-x-2">
          <Button
            variant="outline"
            onClick={resetForm}
            leftIcon={<RotateCcw className="w-4 h-4" />}
          >
            초기화
          </Button>
          <Button
            onClick={savePolicy}
            loading={isSaving}
            leftIcon={<Save className="w-4 h-4" />}
          >
            저장
          </Button>
        </div>
      }
    >
      {/* 알림 메시지 */}
      {error && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-md">
          <p className="text-sm text-red-600">{error}</p>
        </div>
      )}

      {success && (
        <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded-md">
          <p className="text-sm text-green-600">{success}</p>
        </div>
      )}

      <div className="space-y-6">
        {/* 일반 설정 */}
        <Card title="일반 설정" subtitle="기본 정책 정보">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <Input
              label="정책 이름"
              value={formData.name || ''}
              onChange={(e) => updateFormData({ name: e.target.value })}
              placeholder="정책 이름을 입력하세요"
            />

            <div className="flex items-center space-x-4">
              <label className="flex items-center">
                <input
                  type="checkbox"
                  checked={formData.enableArchiving ?? false}
                  onChange={(e) => updateFormData({ enableArchiving: e.target.checked })}
                  className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                />
                <span className="ml-2 text-sm font-medium text-gray-700">
                  아카이빙 활성화
                </span>
              </label>

              <label className="flex items-center">
                <input
                  type="checkbox"
                  checked={formData.enableDataDeletion ?? false}
                  onChange={(e) => updateFormData({ enableDataDeletion: e.target.checked })}
                  className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                />
                <span className="ml-2 text-sm font-medium text-gray-700">
                  데이터 삭제 활성화
                </span>
              </label>
            </div>
          </div>
        </Card>

        {/* 보존 기간 설정 */}
        <Card title="보존 기간 설정" subtitle="데이터 보존 기간을 설정합니다">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <Input
              label="DB 보존 기간 (일)"
              type="number"
              value={formData.dbRetentionDays || ''}
              onChange={(e) => updateFormData({ dbRetentionDays: Number(e.target.value) })}
              placeholder="90"
              helperText="데이터베이스에 보관할 일수"
            />

            <Input
              label="전체 보존 기간 (일)"
              type="number"
              value={formData.totalRetentionDays || ''}
              onChange={(e) => updateFormData({ totalRetentionDays: Number(e.target.value) })}
              placeholder="365"
              helperText="아카이브 포함 전체 보관 일수"
            />
          </div>

          {formData.dbRetentionDays && formData.totalRetentionDays && (
            <div className="mt-4 p-4 bg-blue-50 rounded-lg">
              <p className="text-sm text-blue-800">
                📋 {formData.dbRetentionDays}일 후 데이터가 아카이브되며, 
                총 {formData.totalRetentionDays}일간 보존됩니다.
              </p>
            </div>
          )}
        </Card>

        {/* 아카이빙 설정 */}
        <Card title="아카이빙 설정" subtitle="아카이브 파일 및 배치 처리 설정">
          <div className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <Input
                label="배치 크기"
                type="number"
                value={formData.batchSize || ''}
                onChange={(e) => updateFormData({ batchSize: Number(e.target.value) })}
                placeholder="1000"
                helperText="한 번에 처리할 레코드 수"
              />

              <Input
                label="아카이브 파일 형식"
                value={formData.archiveFileFormat || ''}
                onChange={(e) => updateFormData({ archiveFileFormat: e.target.value })}
                placeholder="yyyy-MM-dd"
                helperText="날짜 형식 패턴"
              />
            </div>

            <Input
              label="아카이브 경로"
              value={formData.archiveBasePath || ''}
              onChange={(e) => updateFormData({ archiveBasePath: e.target.value })}
              placeholder="/tmp/readfast/archive"
              helperText="아카이브 파일을 저장할 경로"
            />
          </div>
        </Card>

        {/* 스케줄 설정 */}
        <Card title="스케줄 설정" subtitle="자동 실행 스케줄을 설정합니다">
          <div className="space-y-4">
            <Input
              label="Cron 표현식"
              value={formData.cronExpression || ''}
              onChange={(e) => updateFormData({ cronExpression: e.target.value })}
              placeholder="0 0 2 * * ?"
              helperText="아카이빙 실행 스케줄 (예: 0 0 2 * * ? = 매일 새벽 2시)"
            />

            {formData.cronExpression && (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="p-3 bg-gray-50 rounded">
                  <p className="text-sm font-medium text-gray-700">설명</p>
                  <p className="text-sm text-gray-600">
                    {PolicyService.parseCronExpression(formData.cronExpression)}
                  </p>
                </div>

                {nextExecutionTime && (
                  <div className="p-3 bg-blue-50 rounded">
                    <p className="text-sm font-medium text-blue-700">다음 실행 예정</p>
                    <p className="text-sm text-blue-600">
                      {nextExecutionTime.toLocaleString('ko-KR')}
                    </p>
                  </div>
                )}
              </div>
            )}

            {/* 일반적인 스케줄 예시 */}
            <div className="p-4 bg-gray-50 rounded-lg">
              <p className="text-sm font-medium text-gray-700 mb-2">일반적인 스케줄 예시:</p>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-2 text-sm text-gray-600">
                <div>• <code>0 0 2 * * ?</code> - 매일 새벽 2시</div>
                <div>• <code>0 0 1 * * ?</code> - 매일 새벽 1시</div>
                <div>• <code>0 0 0 * * SUN</code> - 매주 일요일 자정</div>
                <div>• <code>0 0 0 1 * ?</code> - 매월 1일 자정</div>
              </div>
            </div>
          </div>
        </Card>
      </div>
    </PageContainer>
  );
};