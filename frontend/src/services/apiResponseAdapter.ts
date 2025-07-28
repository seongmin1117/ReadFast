import type { 
  BackendApiResponse, 
  ApiResponse 
} from '@/types/api.types';
import type { 
  BackendAuthLog, 
  AuthLog, 
  BackendPageResponse, 
  PageResponse
} from '@/types/auth.types';

/**
 * API Response 어댑터
 * 백엔드의 실제 응답 형식을 프론트엔드가 기대하는 형식으로 변환
 */
export class ApiResponseAdapter {
  /**
   * 백엔드 API 응답을 프론트엔드 형식으로 변환
   */
  static adaptApiResponse<T>(backendResponse: BackendApiResponse<T>): ApiResponse<T> {
    return {
      success: backendResponse.internalCode === 0,
      data: backendResponse.data,
      message: backendResponse.internalCodeDescription,
      timestamp: backendResponse.dateTime
    };
  }

  /**
   * 백엔드 AuthLog를 프론트엔드 AuthLog로 변환
   */
  static adaptAuthLog(backendAuthLog: BackendAuthLog): AuthLog {
    return {
      id: backendAuthLog.id,
      date: backendAuthLog.date, // ISO 8601 형식 그대로 유지
      device: backendAuthLog.device,
      userId: backendAuthLog.userId,
      result: this.convertAuthResult(backendAuthLog.result),
      endpoint: backendAuthLog.endpoint
    };
  }

  /**
   * 백엔드 AuthLog 배열을 프론트엔드 AuthLog 배열로 변환
   */
  static adaptAuthLogArray(backendAuthLogs: BackendAuthLog[]): AuthLog[] {
    return backendAuthLogs.map(log => this.adaptAuthLog(log));
  }

  /**
   * 백엔드 페이지 응답을 프론트엔드 페이지 응답으로 변환 (커서 기반)
   */
  static adaptPageResponse(
    backendPageResponse: BackendPageResponse<BackendAuthLog>
  ): PageResponse<AuthLog> {
    const content = this.adaptAuthLogArray(backendPageResponse.content);

    return {
      content,
      size: backendPageResponse.size,
      hasNext: backendPageResponse.hasNext,
      isCursorBased: backendPageResponse.isCursorBased
    };
  }

  /**
   * 백엔드 인증 결과를 프론트엔드 형식으로 변환
   */
  private static convertAuthResult(backendResult: BackendAuthLog['result']): AuthLog['result'] {
    const resultMap: Record<BackendAuthLog['result'], AuthLog['result']> = {
      'SUCCESS': 'success',
      'FAIL': 'failure',
      'BLOCKED': 'blocked',
      'EXPIRED': 'expired'
    };

    const normalizedResult = backendResult.toUpperCase() as BackendAuthLog['result'];
    return resultMap[normalizedResult] || 'failure';
  }

  /**
   * 날짜 형식 검증 및 변환
   */
  static validateAndFormatDate(dateStr: string): string {
    try {
      const date = new Date(dateStr);
      if (isNaN(date.getTime())) {
        console.warn(`Invalid date format: ${dateStr}`);
        return new Date().toISOString();
      }
      return date.toISOString();
    } catch (error) {
      console.error('Date parsing error:', error);
      return new Date().toISOString();
    }
  }

  /**
   * 페이지네이션 API 응답 어댑터
   */
  static adaptVersionedResponse(
    backendResponse: BackendApiResponse<BackendPageResponse<BackendAuthLog>>
  ): ApiResponse<PageResponse<AuthLog>> {
    const adaptedApiResponse = this.adaptApiResponse(backendResponse);
    const adaptedPageResponse = this.adaptPageResponse(backendResponse.data);

    return {
      ...adaptedApiResponse,
      data: adaptedPageResponse
    };
  }

}

/**
 * 에러 응답 어댑터
 */
export class ApiErrorAdapter {
  static adaptError(error: any): ApiResponse<null> {
    if (error.response?.data) {
      const backendError = error.response.data as BackendApiResponse<null>;
      return ApiResponseAdapter.adaptApiResponse(backendError);
    }

    return {
      success: false,
      data: null,
      message: error.message || '알 수 없는 오류가 발생했습니다.',
      timestamp: new Date().toISOString()
    };
  }
}