import axios, { AxiosInstance, AxiosResponse, AxiosError } from 'axios';
import type { ApiResponse, RequestConfig } from '@/types/api.types';

// API 베이스 설정
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';
const API_TIMEOUT = 30000; // 30초

// Axios 인스턴스 생성
export const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: API_TIMEOUT,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 요청 인터셉터
apiClient.interceptors.request.use(
  (config) => {
    // 요청 로깅
    console.log(`[API Request] ${config.method?.toUpperCase()} ${config.url}`, {
      params: config.params,
      data: config.data,
    });

    // 인증 토큰 추가 (향후 확장)
    // const token = localStorage.getItem('accessToken');
    // if (token) {
    //   config.headers.Authorization = `Bearer ${token}`;
    // }

    return config;
  },
  (error) => {
    console.error('[API Request Error]', error);
    return Promise.reject(error);
  }
);

// 응답 인터셉터
apiClient.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    console.log(`[API Response] ${response.config.method?.toUpperCase()} ${response.config.url}`, {
      status: response.status,
      data: response.data,
    });

    return response;
  },
  (error: AxiosError<ApiResponse>) => {
    const { response } = error;

    console.error('[API Response Error]', {
      url: error.config?.url,
      method: error.config?.method,
      status: response?.status,
      data: response?.data,
    });

    // 공통 에러 처리
    if (response && response.status === 401) {
      // 인증 에러 처리
      console.warn('Authentication failed, redirecting to login...');
      // window.location.href = '/login';
    } else if (response && response.status === 403) {
      // 권한 에러 처리
      console.warn('Access forbidden');
    } else if (response && response.status >= 500) {
      // 서버 에러 처리
      console.error('Server error occurred');
    }

    return Promise.reject(error);
  }
);

// API 호출 헬퍼 함수들
export class ApiService {
  // GET 요청
  static async get<T = any>(
    url: string,
    params?: Record<string, any>,
    config?: RequestConfig
  ): Promise<ApiResponse<T>> {
    try {
      const response = await apiClient.get<ApiResponse<T>>(url, {
        params,
        timeout: config?.timeout,
        headers: config?.headers,
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error as AxiosError<ApiResponse>);
    }
  }

  // POST 요청
  static async post<T = any>(
    url: string,
    data?: any,
    config?: RequestConfig
  ): Promise<ApiResponse<T>> {
    try {
      const response = await apiClient.post<ApiResponse<T>>(url, data, {
        timeout: config?.timeout,
        headers: config?.headers,
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error as AxiosError<ApiResponse>);
    }
  }

  // PUT 요청
  static async put<T = any>(
    url: string,
    data?: any,
    config?: RequestConfig
  ): Promise<ApiResponse<T>> {
    try {
      const response = await apiClient.put<ApiResponse<T>>(url, data, {
        timeout: config?.timeout,
        headers: config?.headers,
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error as AxiosError<ApiResponse>);
    }
  }

  // DELETE 요청
  static async delete<T = any>(
    url: string,
    config?: RequestConfig
  ): Promise<ApiResponse<T>> {
    try {
      const response = await apiClient.delete<ApiResponse<T>>(url, {
        timeout: config?.timeout,
        headers: config?.headers,
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error as AxiosError<ApiResponse>);
    }
  }

  // 에러 처리
  private static handleError(error: AxiosError<ApiResponse>): Error {
    const { response } = error;

    if (response?.data?.message) {
      return new Error(response.data.message);
    }

    if (error.code === 'ECONNABORTED') {
      return new Error('요청 시간이 초과되었습니다.');
    }

    if (!response) {
      return new Error('네트워크 연결을 확인해주세요.');
    }

    const statusMessages: Record<number, string> = {
      400: '잘못된 요청입니다.',
      401: '인증이 필요합니다.',
      403: '권한이 없습니다.',
      404: '요청한 리소스를 찾을 수 없습니다.',
      409: '요청이 충돌했습니다.',
      422: '입력값을 확인해주세요.',
      429: '요청 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.',
      500: '서버에 오류가 발생했습니다.',
      502: '서버 게이트웨이 오류입니다.',
      503: '서비스를 사용할 수 없습니다.',
      504: '게이트웨이 응답 시간이 초과되었습니다.',
    };

    const message = statusMessages[response.status] || '알 수 없는 오류가 발생했습니다.';
    return new Error(message);
  }
}

// 캐시 관리 클래스
export class ApiCache {
  private static cache = new Map<string, { data: any; timestamp: number; expiry: number }>();

  static set<T>(key: string, data: T, ttl: number = 300000): void { // 기본 5분
    const now = Date.now();
    this.cache.set(key, {
      data,
      timestamp: now,
      expiry: now + ttl,
    });
  }

  static get<T>(key: string): T | null {
    const cached = this.cache.get(key);
    if (!cached) return null;

    const now = Date.now();
    if (now > cached.expiry) {
      this.cache.delete(key);
      return null;
    }

    return cached.data as T;
  }

  static clear(pattern?: string): void {
    if (!pattern) {
      this.cache.clear();
      return;
    }

    const regex = new RegExp(pattern);
    for (const key of this.cache.keys()) {
      if (regex.test(key)) {
        this.cache.delete(key);
      }
    }
  }

  static invalidate(key: string): void {
    this.cache.delete(key);
  }
}