import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Layout } from '@/components/layout';

// 페이지 컴포넌트들 (Lazy Loading)
const Dashboard = React.lazy(() => import('@/pages/Dashboard').then(module => ({ default: module.Dashboard })));
const AuthLogList = React.lazy(() => import('@/pages/AuthLogs/AuthLogList').then(module => ({ default: module.AuthLogList })));
const ArchivingDashboard = React.lazy(() => import('@/pages/Archiving/ArchivingDashboard').then(module => ({ default: module.ArchivingDashboard })));
const PolicySettings = React.lazy(() => import('@/pages/Settings/PolicySettings').then(module => ({ default: module.PolicySettings })));

// 로딩 컴포넌트
const PageLoader: React.FC = () => (
  <div className="min-h-[400px] flex items-center justify-center">
    <div className="flex items-center space-x-2">
      <div className="w-8 h-8 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin"></div>
      <span className="text-gray-600">로딩 중...</span>
    </div>
  </div>
);

// 404 페이지
const NotFound: React.FC = () => (
  <div className="min-h-[400px] flex items-center justify-center">
    <div className="text-center">
      <div className="mb-4">
        <div className="w-16 h-16 mx-auto bg-gray-100 rounded-full flex items-center justify-center">
          <svg className="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9.172 16.172a4 4 0 015.656 0M9 12h6m-6-4h6m2 5.291A7.962 7.962 0 0112 20a7.962 7.962 0 01-6-2.709V17h.001M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
      </div>
      <h1 className="text-2xl font-semibold text-gray-900 mb-2">
        페이지를 찾을 수 없습니다
      </h1>
      <p className="text-gray-600 mb-6">
        요청하신 페이지가 존재하지 않거나 이동되었을 수 있습니다.
      </p>
      <button
        onClick={() => window.history.back()}
        className="bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 px-4 rounded-md transition-colors mr-3"
      >
        이전 페이지
      </button>
      <a
        href="/"
        className="bg-gray-100 hover:bg-gray-200 text-gray-700 font-medium py-2 px-4 rounded-md transition-colors"
      >
        홈으로
      </a>
    </div>
  </div>
);

function App() {
  return (
    <Layout>
      <React.Suspense fallback={<PageLoader />}>
        <Routes>
          {/* 대시보드 */}
          <Route path="/" element={<Dashboard />} />
          
          {/* 인증 로그 */}
          <Route path="/auth-logs" element={<AuthLogList />} />
          
          {/* 데이터 아카이빙 */}
          <Route path="/archiving" element={<ArchivingDashboard />} />
          
          {/* 분석 및 통계 - 향후 구현 */}
          <Route 
            path="/analytics" 
            element={
              <div className="text-center py-12">
                <h2 className="text-xl font-semibold mb-2">분석 및 통계</h2>
                <p className="text-gray-600">이 기능은 곧 제공될 예정입니다.</p>
              </div>
            } 
          />
          
          {/* 보안 모니터링 - 향후 구현 */}
          <Route 
            path="/security" 
            element={
              <div className="text-center py-12">
                <h2 className="text-xl font-semibold mb-2">보안 모니터링</h2>
                <p className="text-gray-600">이 기능은 곧 제공될 예정입니다.</p>
              </div>
            } 
          />
          
          {/* 설정 */}
          <Route path="/settings" element={<PolicySettings />} />
          
          {/* 리다이렉션 */}
          <Route path="/dashboard" element={<Navigate to="/" replace />} />
          
          {/* 404 */}
          <Route path="*" element={<NotFound />} />
        </Routes>
      </React.Suspense>
    </Layout>
  );
}

export default App;