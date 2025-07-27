package com.baro13.readfast.admin.authlog.adapter.out.batch.processor;

import com.baro13.readfast.admin.authlog.adapter.out.archive.storage.Storage;
import com.baro13.readfast.admin.authlog.adapter.out.archive.storage.StorageFactory;
import com.baro13.readfast.admin.authlog.adapter.out.batch.metadata.ArchiveMetadataManager;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.baro13.readfast.admin.policy.domain.model.DataRetentionPolicy;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 청크 단위 데이터 처리자
 * 개별 청크의 아카이빙 처리를 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChunkProcessor {
    
    private final StorageFactory storageFactory;
    private final ArchiveMetadataManager metadataManager;

    /**
     * 청크 데이터 처리
     */
    public ChunkResult processChunk(List<AuthLog> chunkData, DataRetentionPolicy policy, int chunkNumber) {
        
        log.info("청크 #{} 처리 시작 - 데이터 건수: {}", chunkNumber, chunkData.size());
        
        try {
            // 스토리지 해결
            var storage = storageFactory.resolve();
            
            // 데이터 아카이빙
            var archivedCount = archiveChunkData(chunkData, storage);
            
            // 메타데이터 저장
            metadataManager.saveArchiveMetadata(policy, chunkData, storage, chunkNumber);
            
            // 데이터 삭제 (정책에 따라)
            var deletedCount = deleteChunkDataIfEnabled(chunkData, policy, chunkNumber);
            
            var result = new ChunkResult(chunkData.size(), archivedCount, deletedCount);
            
            log.info("청크 #{} 처리 완료 - Processed: {}, Archived: {}, Deleted: {}", 
                    chunkNumber, result.processedCount(), result.archivedCount(), result.deletedCount());
            
            return result;
            
        } catch (Exception e) {
            log.error("청크 #{} 처리 실패. 데이터 건수: {}", chunkNumber, chunkData.size(), e);
            return new ChunkResult(chunkData.size(), 0, 0);
        }
    }
    
    private long archiveChunkData(List<AuthLog> chunkData, Storage storage) {
        try {
            var currentDate = LocalDate.now();
            storage.store(chunkData, currentDate);
            return chunkData.size();
            
        } catch (Exception e) {
            log.error("청크 데이터 아카이빙 실패. 건수: {}", chunkData.size(), e);
            return 0;
        }
    }
    
    private long deleteChunkDataIfEnabled(List<AuthLog> chunkData, DataRetentionPolicy policy, int chunkNumber) {
        if (!policy.getRetentionRule().isEnableDataDeletion()) {
            return 0;
        }
        
        try {
            // MVP: 실제 삭제는 구현하지 않음 (데이터 안전을 위해)
            // TODO: 실제 배포 시에는 authLogDbReader.deleteByIds(chunkData.ids()) 구현
            var deletedCount = chunkData.size();
            log.debug("청크 #{} - 데이터 삭제 시뮬레이션: {}건", chunkNumber, deletedCount);
            return deletedCount;
            
        } catch (Exception e) {
            log.error("청크 #{} 데이터 삭제 실패. 건수: {}", chunkNumber, chunkData.size(), e);
            return 0;
        }
    }
}