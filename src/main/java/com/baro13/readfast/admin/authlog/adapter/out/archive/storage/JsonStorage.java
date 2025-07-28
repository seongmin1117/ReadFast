package com.baro13.readfast.admin.authlog.adapter.out.archive.storage;

import com.baro13.readfast.admin.authlog.adapter.out.archive.compression.CompressionFactory;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.baro13.readfast.admin.authlog.domain.port.DataRetentionPolicyProvider;
import com.baro13.readfast.admin.policy.domain.model.vo.ArchivingStrategy.ArchiveFormat;
import com.baro13.readfast.admin.policy.domain.model.vo.ArchivingStrategy.CompressionType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * JSON 형태로 데이터를 저장하는 스토리지 구현체
 * 압축 기능과 통합되어 효율적인 파일 저장/조회를 지원
 */
@Slf4j
@Repository
public class JsonStorage implements Storage {

    private final DataRetentionPolicyProvider policyProvider;
    private final CompressionFactory compressionFactory;
    private final ObjectMapper objectMapper;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    public JsonStorage(DataRetentionPolicyProvider policyProvider, CompressionFactory compressionFactory) {
        this.policyProvider = policyProvider;
        this.compressionFactory = compressionFactory;
        this.objectMapper = createObjectMapper();
    }
    
    private static ObjectMapper createObjectMapper() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Override
    public void store(List<AuthLog> authLogs, LocalDate date) {
        if (authLogs == null || authLogs.isEmpty()) {
            log.warn("저장할 인증 로그가 없습니다. date: {}", date);
            return;
        }
        
        var jsonPath = getJsonPath(date);
        
        try {
            createDirectoryIfNotExists(jsonPath.getParent());
            
            // AuthLog를 JSON 직렬화 가능한 형태로 변환
            var jsonData = authLogs.stream()
                .map(this::convertToJsonData)
                .collect(Collectors.toList());
            
            // JSON으로 직렬화
            var jsonBytes = objectMapper.writeValueAsBytes(jsonData);
            
            // 압축 적용 (정책에 따라)
            var finalBytes = applyCompressionIfEnabled(jsonBytes, jsonPath);
            var finalPath = isCompressionEnabled() ? getCompressedPath(jsonPath) : jsonPath;
            
            // 파일 저장
            Files.write(finalPath, finalBytes);
            
            var compressionRatio = jsonBytes.length > 0 ? (double) finalBytes.length / jsonBytes.length * 100 : 100;
            log.info("JSON 파일 저장 완료. 건수: {}, 원본: {}bytes, 최종: {}bytes, 압축비: {:.1f}%, 파일: {}", 
                    authLogs.size(), jsonBytes.length, finalBytes.length, compressionRatio, finalPath);
                    
        } catch (Exception e) {
            log.error("JSON 데이터 저장 실패. 건수: {}", authLogs.size(), e);
            throw new RuntimeException("JSON 데이터 저장 중 오류 발생", e);
        }
    }

    @Override
    public List<AuthLog> retrieve(LocalDate date) {
        var jsonPath = getJsonPath(date);
        var actualPath = determineActualPath(jsonPath);
        
        if (actualPath == null || !Files.exists(actualPath)) {
            log.debug("JSON 파일이 존재하지 않습니다. 파일: {}", jsonPath);
            return List.of();
        }
        
        try {
            // 파일 읽기
            var fileBytes = Files.readAllBytes(actualPath);
            
            // 압축 해제 (필요한 경우)
            var jsonBytes = decompressIfNeeded(fileBytes, actualPath);
            
            // JSON 역직렬화
            var jsonDataList = objectMapper.readValue(jsonBytes, new TypeReference<List<AuthLogJsonData>>() {});
            
            // AuthLog 객체로 변환
            var authLogs = jsonDataList.stream()
                .map(this::convertFromJsonData)
                .collect(Collectors.toList());
            
            log.debug("JSON 파일에서 인증 로그 조회 완료. 파일: {}, 건수: {}", actualPath, authLogs.size());
            return authLogs;
            
        } catch (Exception e) {
            log.error("JSON 데이터 조회 실패. 날짜: {}", date, e);
            throw new RuntimeException("JSON 데이터 조회 중 오류 발생", e);
        }
    }

    @Override
    public List<AuthLog> retrieveByDateRange(LocalDate startDate, LocalDate endDate) {
        var allLogs = new ArrayList<AuthLog>();
        var currentDate = startDate;
        
        while (!currentDate.isAfter(endDate)) {
            allLogs.addAll(retrieve(currentDate));
            currentDate = currentDate.plusDays(1);
        }
        
        log.debug("JSON 날짜 범위 조회 완료. 기간: {} ~ {}, 총 건수: {}", 
                 startDate, endDate, allLogs.size());
        
        return allLogs;
    }

    @Override
    public boolean exists(LocalDate date) {
        var jsonPath = getJsonPath(date);
        var compressedPath = getCompressedPath(jsonPath);
        
        return (Files.exists(jsonPath) && Files.isRegularFile(jsonPath)) ||
               (Files.exists(compressedPath) && Files.isRegularFile(compressedPath));
    }

    @Override
    public void delete(LocalDate date) {
        var jsonPath = getJsonPath(date);
        var compressedPath = getCompressedPath(jsonPath);
        
        try {
            var deleted = false;
            
            if (Files.exists(jsonPath)) {
                Files.delete(jsonPath);
                log.info("JSON 파일 삭제 완료. 파일: {}", jsonPath);
                deleted = true;
            }
            
            if (Files.exists(compressedPath)) {
                Files.delete(compressedPath);
                log.info("압축된 JSON 파일 삭제 완료. 파일: {}", compressedPath);
                deleted = true;
            }
            
            if (!deleted) {
                log.debug("삭제할 JSON 파일이 존재하지 않습니다. 경로: {} 또는 {}", jsonPath, compressedPath);
            }
        } catch (IOException e) {
            log.error("JSON 파일 삭제 실패. 날짜: {}", date, e);
            throw new RuntimeException("JSON 파일 삭제 중 오류 발생", e);
        }
    }

    @Override
    public ArchiveFormat getArchiveFormat() {
        return ArchiveFormat.JSON;
    }

    @Override
    public List<AuthLog> findByCursor(LocalDate cursorDate, int limit) {
        return List.of();
    }

    // === Private Methods ===
    
    private Path getJsonPath(LocalDate date) {
        var archiveBasePath = policyProvider.getCurrentPolicy()
            .getArchivingStrategy()
            .getArchiveBasePath();
        
        var dateDir = date.format(DATE_FORMATTER);
        var fileName = dateDir + ".json";
        
        return Paths.get(archiveBasePath, dateDir, fileName);
    }
    
    private Path getCompressedPath(Path originalPath) {
        var compressionType = policyProvider.getCurrentPolicy()
            .getArchivingStrategy()
            .getCompressionType();
        
        return Paths.get(originalPath.toString() + compressionType.getExtension());
    }
    
    private boolean isCompressionEnabled() {
        return policyProvider.getCurrentPolicy()
            .getArchivingStrategy()
            .getCompressionType() != CompressionType.NONE;
    }
    
    private Path determineActualPath(Path originalPath) {
        if (isCompressionEnabled()) {
            var compressedPath = getCompressedPath(originalPath);
            if (Files.exists(compressedPath)) {
                return compressedPath;
            }
        }
        
        return Files.exists(originalPath) ? originalPath : null;
    }
    
    private byte[] applyCompressionIfEnabled(byte[] data, Path originalPath) {
        try {
            if (!isCompressionEnabled()) {
                return data;
            }
            
            var compression = compressionFactory.resolve();
            return compression.compress(data);
            
        } catch (Exception e) {
            log.error("JSON 데이터 압축 실패. 파일: {}", originalPath, e);
            return data; // 압축 실패 시 원본 데이터 반환
        }
    }
    
    private byte[] decompressIfNeeded(byte[] data, Path filePath) {
        try {
            if (!isCompressionEnabled()) {
                return data;
            }
            
            // 압축된 파일인지 확인 (확장자 기반)
            var compressionExtension = policyProvider.getCurrentPolicy()
                .getArchivingStrategy()
                .getCompressionType()
                .getExtension();
            
            if (filePath.toString().endsWith(compressionExtension)) {
                var compression = compressionFactory.resolve();
                return compression.decompress(data);
            }
            
            return data;
            
        } catch (Exception e) {
            log.error("JSON 데이터 압축 해제 실패. 파일: {}", filePath, e);
            return data; // 압축 해제 실패 시 원본 데이터 반환
        }
    }
    
    private void createDirectoryIfNotExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
    }
    
    private AuthLogJsonData convertToJsonData(AuthLog authLog) {
        return new AuthLogJsonData(
            authLog.getId(),
            authLog.getDate().toString(),
            authLog.getDevice(),
            authLog.getUserId(),
            authLog.getResult(),
            authLog.getEndpoint()
        );
    }
    
    private AuthLog convertFromJsonData(AuthLogJsonData jsonData) {
        return AuthLog.of(
            jsonData.id(),
            Instant.parse(jsonData.date()),
            jsonData.device(),
            jsonData.userId(),
            jsonData.result(),
            jsonData.endpoint()
        );
    }
    
    /**
     * JSON 직렬화용 데이터 클래스
     */
    private record AuthLogJsonData(
        Long id,
        String date,
        String device,
        String userId,
        String result,
        String endpoint
    ) {}
}