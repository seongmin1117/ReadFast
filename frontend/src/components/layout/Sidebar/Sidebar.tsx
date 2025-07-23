import React from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { 
  LayoutDashboard,
  FileSearch,
  Database,
  Settings,
  BarChart3,
  Shield,
  X
} from 'lucide-react';
import { cn } from '@/utils/cn.utils';
import { Button } from '@/components/ui/Button';
import type { MenuItem } from '@/types/common.types';

interface SidebarProps {
  isOpen?: boolean;
  onClose?: () => void;
  className?: string;
}

const menuItems: MenuItem[] = [
  {
    id: 'dashboard',
    label: '대시보드',
    icon: 'LayoutDashboard',
    path: '/',
  },
  {
    id: 'auth-logs',
    label: '인증 로그',
    icon: 'FileSearch',
    path: '/auth-logs',
    badge: 'New'
  },
  {
    id: 'archiving',
    label: '데이터 아카이빙',
    icon: 'Database',
    path: '/archiving',
  },
  {
    id: 'analytics',
    label: '분석 및 통계',
    icon: 'BarChart3',
    path: '/analytics',
  },
  {
    id: 'security',
    label: '보안 모니터링',
    icon: 'Shield',
    path: '/security',
  },
  {
    id: 'settings',
    label: '설정',
    icon: 'Settings',
    path: '/settings',
  },
];

const iconMap = {
  LayoutDashboard,
  FileSearch,
  Database,
  BarChart3,
  Shield,
  Settings,
};

export const Sidebar: React.FC<SidebarProps> = ({
  isOpen = true,
  onClose,
  className,
}) => {
  const location = useLocation();

  const renderIcon = (iconName: string) => {
    const IconComponent = iconMap[iconName as keyof typeof iconMap];
    return IconComponent ? <IconComponent className="w-5 h-5" /> : null;
  };

  const isActiveRoute = (path: string) => {
    if (path === '/') {
      return location.pathname === '/';
    }
    return location.pathname.startsWith(path);
  };

  return (
    <>
      {/* Mobile overlay */}
      {isOpen && onClose && (
        <div
          className="fixed inset-0 bg-black/50 z-40 lg:hidden"
          onClick={onClose}
        />
      )}

      {/* Sidebar */}
      <aside
        className={cn(
          'fixed top-0 left-0 z-50 h-full w-64 bg-white border-r border-gray-200 transform transition-transform duration-300 ease-in-out lg:translate-x-0 lg:z-40',
          isOpen ? 'translate-x-0' : '-translate-x-full',
          className
        )}
      >
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200">
          <div className="flex items-center space-x-2">
            <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
              <FileSearch className="w-4 h-4 text-white" />
            </div>
            <span className="text-xl font-bold text-gray-900">ReadFast</span>
          </div>
          
          {onClose && (
            <Button
              variant="ghost"
              size="sm"
              className="p-2 lg:hidden"
              onClick={onClose}
              aria-label="사이드바 닫기"
            >
              <X className="w-4 h-4" />
            </Button>
          )}
        </div>

        {/* Navigation */}
        <nav className="p-4">
          <ul className="space-y-2">
            {menuItems.map((item) => (
              <li key={item.id}>
                <NavLink
                  to={item.path || '#'}
                  className={({ isActive }) =>
                    cn(
                      'flex items-center justify-between px-4 py-2.5 rounded-lg text-sm font-medium transition-colors group',
                      isActive || isActiveRoute(item.path || '')
                        ? 'bg-blue-50 text-blue-700 border border-blue-200'
                        : 'text-gray-700 hover:bg-gray-100 hover:text-gray-900'
                    )
                  }
                >
                  <div className="flex items-center space-x-3">
                    {renderIcon(item.icon || '')}
                    <span>{item.label}</span>
                  </div>
                  
                  {item.badge && (
                    <span className="bg-blue-100 text-blue-700 text-xs px-2 py-1 rounded-full">
                      {item.badge}
                    </span>
                  )}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>

        {/* Footer */}
        <div className="absolute bottom-0 left-0 right-0 p-4 border-t border-gray-200">
          <div className="text-center text-xs text-gray-500">
            <p>ReadFast Admin v1.0</p>
            <p className="mt-1">© 2024 ReadFast Inc.</p>
          </div>
        </div>
      </aside>
    </>
  );
};