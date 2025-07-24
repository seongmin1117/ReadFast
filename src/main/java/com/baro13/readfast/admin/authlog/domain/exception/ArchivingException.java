package com.baro13.readfast.admin.authlog.domain.exception;

/**
 * 아카이빙 관련 비즈니스 예외
 */
public class ArchivingException extends RuntimeException {
    
    public ArchivingException(String message) {
        super(message);
    }
    
    public ArchivingException(String message, Throwable cause) {
        super(message, cause);
    }
}