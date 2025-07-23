import React, { useState } from 'react';
import { cn } from '@/utils/cn.utils';
import { Header } from '../Header';
import { Sidebar } from '../Sidebar';

interface LayoutProps {
  children: React.ReactNode;
  title?: string;
  className?: string;
  headerClassName?: string;
  contentClassName?: string;
}

export const Layout: React.FC<LayoutProps> = ({
  children,
  title,
  className,
  headerClassName,
  contentClassName,
}) => {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  const toggleSidebar = () => {
    setIsSidebarOpen(!isSidebarOpen);
  };

  const closeSidebar = () => {
    setIsSidebarOpen(false);
  };

  return (
    <div className={cn('min-h-screen bg-gray-50', className)}>
      {/* Sidebar */}
      <Sidebar 
        isOpen={isSidebarOpen} 
        onClose={closeSidebar}
      />

      {/* Main Content */}
      <div className="lg:ml-64">
        {/* Header */}
        <Header
          onMenuClick={toggleSidebar}
          title={title}
          className={headerClassName}
        />

        {/* Page Content */}
        <main className={cn('px-4 py-6 lg:px-8', contentClassName)}>
          {children}
        </main>
      </div>
    </div>
  );
};

// 페이지 컨테이너 컴포넌트
export const PageContainer: React.FC<{
  children: React.ReactNode;
  title?: string;
  description?: string;
  action?: React.ReactNode;
  breadcrumbs?: Array<{ label: string; href?: string }>;
  className?: string;
}> = ({
  children,
  title,
  description,
  action,
  breadcrumbs,
  className,
}) => {
  return (
    <div className={cn('space-y-6', className)}>
      {/* Page Header */}
      {(title || description || action || breadcrumbs) && (
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          {/* Breadcrumbs */}
          {breadcrumbs && breadcrumbs.length > 0 && (
            <nav className="mb-4">
              <ol className="flex items-center space-x-2 text-sm text-gray-500">
                {breadcrumbs.map((crumb, index) => (
                  <li key={index} className="flex items-center">
                    {index > 0 && <span className="mx-2">/</span>}
                    {crumb.href ? (
                      <a 
                        href={crumb.href}
                        className="hover:text-gray-700"
                      >
                        {crumb.label}
                      </a>
                    ) : (
                      <span className="font-medium text-gray-900">
                        {crumb.label}
                      </span>
                    )}
                  </li>
                ))}
              </ol>
            </nav>
          )}

          {/* Title and Action */}
          <div className="flex items-start justify-between">
            <div>
              {title && (
                <h1 className="text-2xl font-bold text-gray-900 mb-2">
                  {title}
                </h1>
              )}
              {description && (
                <p className="text-gray-600">
                  {description}
                </p>
              )}
            </div>
            
            {action && (
              <div className="ml-4 flex-shrink-0">
                {action}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Page Content */}
      <div>
        {children}
      </div>
    </div>
  );
};

// 카드 컨테이너 컴포넌트
export const Card: React.FC<{
  children: React.ReactNode;
  title?: string;
  subtitle?: string;
  action?: React.ReactNode;
  className?: string;
  bodyClassName?: string;
}> = ({
  children,
  title,
  subtitle,
  action,
  className,
  bodyClassName,
}) => {
  return (
    <div className={cn('bg-white rounded-lg border border-gray-200 shadow-sm', className)}>
      {/* Card Header */}
      {(title || subtitle || action) && (
        <div className="px-6 py-4 border-b border-gray-200">
          <div className="flex items-start justify-between">
            <div>
              {title && (
                <h3 className="text-lg font-medium text-gray-900 mb-1">
                  {title}
                </h3>
              )}
              {subtitle && (
                <p className="text-sm text-gray-500">
                  {subtitle}
                </p>
              )}
            </div>
            
            {action && (
              <div className="ml-4 flex-shrink-0">
                {action}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Card Body */}
      <div className={cn('p-6', bodyClassName)}>
        {children}
      </div>
    </div>
  );
};