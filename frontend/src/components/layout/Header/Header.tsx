import React from 'react';
import { Menu, Bell, Settings, User, LogOut } from 'lucide-react';
import { cn } from '@/utils/cn.utils';
import { Button } from '@/components/ui/Button';

interface HeaderProps {
  onMenuClick?: () => void;
  title?: string;
  className?: string;
}

export const Header: React.FC<HeaderProps> = ({
  onMenuClick,
  title = 'ReadFast Admin',
  className,
}) => {
  const [notificationCount] = React.useState(3);
  const [isProfileOpen, setIsProfileOpen] = React.useState(false);

  return (
    <header className={cn('bg-white border-b border-gray-200 px-4 py-4 lg:px-6', className)}>
      <div className="flex items-center justify-between">
        {/* Left side */}
        <div className="flex items-center space-x-4">
          {onMenuClick && (
            <Button
              variant="ghost"
              size="sm"
              onClick={onMenuClick}
              className="p-2 lg:hidden"
              aria-label="메뉴 열기"
            >
              <Menu className="w-5 h-5" />
            </Button>
          )}
          
          <div>
            <h1 className="text-xl font-semibold text-gray-900">{title}</h1>
            <p className="text-sm text-gray-500">인증 로그 관리 시스템</p>
          </div>
        </div>

        {/* Right side */}
        <div className="flex items-center space-x-4">
          {/* Notifications */}
          <div className="relative">
            <Button
              variant="ghost"
              size="sm"
              className="p-2 relative"
              aria-label="알림"
            >
              <Bell className="w-5 h-5" />
              {notificationCount > 0 && (
                <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">
                  {notificationCount > 9 ? '9+' : notificationCount}
                </span>
              )}
            </Button>
          </div>

          {/* Settings */}
          <Button
            variant="ghost"
            size="sm"
            className="p-2"
            aria-label="설정"
          >
            <Settings className="w-5 h-5" />
          </Button>

          {/* Profile dropdown */}
          <div className="relative">
            <Button
              variant="ghost"
              size="sm"
              className="p-2 flex items-center space-x-2"
              onClick={() => setIsProfileOpen(!isProfileOpen)}
            >
              <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center">
                <User className="w-4 h-4 text-white" />
              </div>
              <span className="hidden md:block text-sm font-medium">
                Admin
              </span>
            </Button>

            {isProfileOpen && (
              <div className="absolute right-0 top-full mt-2 w-48 bg-white rounded-md shadow-lg border border-gray-200 z-50">
                <div className="py-1">
                  <div className="px-4 py-2 border-b border-gray-200">
                    <p className="text-sm font-medium text-gray-900">관리자</p>
                    <p className="text-sm text-gray-500">admin@readfast.com</p>
                  </div>
                  
                  <button className="flex w-full items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">
                    <User className="w-4 h-4 mr-3" />
                    프로필
                  </button>
                  
                  <button className="flex w-full items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">
                    <Settings className="w-4 h-4 mr-3" />
                    설정
                  </button>
                  
                  <hr className="my-1" />
                  
                  <button className="flex w-full items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">
                    <LogOut className="w-4 h-4 mr-3" />
                    로그아웃
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};