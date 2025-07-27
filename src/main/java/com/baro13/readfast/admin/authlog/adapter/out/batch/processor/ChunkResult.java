package com.baro13.readfast.admin.authlog.adapter.out.batch.processor;

/**
 * 청크 처리 결과
 * 각 청크의 처리 성과를 추적하기 위한 불변 데이터 클래스
 */
public record ChunkResult(
    long processedCount,  // 처리된 레코드 수
    long archivedCount,   // 아카이빙된 레코드 수  
    long deletedCount     // 삭제된 레코드 수
) {
    
    /**
     * 청크 처리 성공 여부 확인
     */
    public boolean isSuccessful() {
        return archivedCount > 0;
    }
    
    /**
     * 청크 처리 효율성 계산 (아카이빙 성공률)
     */
    public double getArchiveSuccessRate() {
        return processedCount > 0 ? (double) archivedCount / processedCount : 0.0;
    }
}