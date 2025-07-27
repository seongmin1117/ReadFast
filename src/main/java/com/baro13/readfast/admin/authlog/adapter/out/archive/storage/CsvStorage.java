package com.baro13.readfast.admin.authlog.adapter.out.archive.storage;

import com.baro13.readfast.admin.authlog.adapter.out.archive.compression.CompressionFactory;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.baro13.readfast.admin.authlog.domain.port.DataRetentionPolicyProvider;
import com.baro13.readfast.admin.policy.domain.model.vo.ArchivingStrategy.ArchiveFormat;
import com.baro13.readfast.admin.policy.domain.model.vo.ArchivingStrategy.CompressionType;
import com.baro13.readfast.global.common.TimeZoneConstants;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * CSV 형태로 데이터를 저장하는 스토리지 구현체
 * 압축 기능과 통합되어 효율적인 파일 저장/조회를 지원
 */
@Slf4j
@Repository
public class CsvStorage implements Storage {

    private final DataRetentionPolicyProvider policyProvider;
    private final CompressionFactory compressionFactory;
    
    public CsvStorage(DataRetentionPolicyProvider policyProvider, CompressionFactory compressionFactory) {
        this.policyProvider = policyProvider;
        this.compressionFactory = compressionFactory;
    }
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String CSV_HEADER = "id,date,device,userId,result,endpoint";
    private static final String CSV_DELIMITER = ",";
    private static final String CSV_QUOTE = "\"";

    @Override
    public void store(List<AuthLog> authLogs, LocalDate date) {
        if (authLogs == null || authLogs.isEmpty()) {
            log.warn("저장할 인증 로그가 없습니다. date: {}", date);
            return;
        }
        
        var csvPath = getCsvPath(date);
        
        try {
            createDirectoryIfNotExists(csvPath.getParent());
            
            // CSV 생성
            var csvContent = generateCsvContent(authLogs);
            var csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);
            
            // 압축 적용 (정책에 따라)
            var finalBytes = applyCompressionIfEnabled(csvBytes, csvPath);
            var finalPath = isCompressionEnabled() ? getCompressedPath(csvPath) : csvPath;
            
            // 파일 저장
            Files.write(finalPath, finalBytes);
            
            var compressionRatio = csvBytes.length > 0 ? (double) finalBytes.length / csvBytes.length * 100 : 100;
            log.info("CSV 파일 저장 완료. 건수: {}, 원본: {}bytes, 최종: {}bytes, 압축비: {:.1f}%, 파일: {}", 
                    authLogs.size(), csvBytes.length, finalBytes.length, compressionRatio, finalPath);
                    
        } catch (Exception e) {
            log.error("CSV 데이터 저장 실패. 건수: {}", authLogs.size(), e);
            throw new RuntimeException("CSV 데이터 저장 중 오류 발생", e);
        }
    }

    @Override
    public List<AuthLog> retrieve(LocalDate date) {
        var csvPath = getCsvPath(date);
        var actualPath = determineActualPath(csvPath);
        
        if (actualPath == null || !Files.exists(actualPath)) {
            log.debug("CSV 파일이 존재하지 않습니다. 파일: {}", csvPath);
            return List.of();
        }
        
        try {
            // 파일 읽기
            var fileBytes = Files.readAllBytes(actualPath);
            
            // 압축 해제 (필요한 경우)
            var csvBytes = decompressIfNeeded(fileBytes, actualPath);
            var csvContent = new String(csvBytes, StandardCharsets.UTF_8);
            
            // CSV 파싱
            var authLogs = parseCsvContent(csvContent);
            
            log.debug("CSV 파일에서 인증 로그 조회 완료. 파일: {}, 건수: {}", actualPath, authLogs.size());
            return authLogs;
            
        } catch (Exception e) {
            log.error("CSV 데이터 조회 실패. 날짜: {}", date, e);
            throw new RuntimeException("CSV 데이터 조회 중 오류 발생", e);
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
        
        log.debug("CSV 날짜 범위 조회 완료. 기간: {} ~ {}, 총 건수: {}", 
                 startDate, endDate, allLogs.size());
        
        return allLogs;
    }

    @Override
    public boolean exists(LocalDate date) {
        var csvPath = getCsvPath(date);
        var compressedPath = getCompressedPath(csvPath);
        
        return (Files.exists(csvPath) && Files.isRegularFile(csvPath)) ||
               (Files.exists(compressedPath) && Files.isRegularFile(compressedPath));
    }

    @Override
    public void delete(LocalDate date) {
        var csvPath = getCsvPath(date);
        var compressedPath = getCompressedPath(csvPath);
        
        try {
            var deleted = false;
            
            if (Files.exists(csvPath)) {
                Files.delete(csvPath);
                log.info("CSV 파일 삭제 완료. 파일: {}", csvPath);
                deleted = true;
            }
            
            if (Files.exists(compressedPath)) {
                Files.delete(compressedPath);
                log.info("압축된 CSV 파일 삭제 완료. 파일: {}", compressedPath);
                deleted = true;
            }
            
            if (!deleted) {
                log.debug("삭제할 CSV 파일이 존재하지 않습니다. 경로: {} 또는 {}", csvPath, compressedPath);
            }
        } catch (IOException e) {
            log.error("CSV 파일 삭제 실패. 날짜: {}", date, e);
            throw new RuntimeException("CSV 파일 삭제 중 오류 발생", e);
        }
    }

    @Override
    public ArchiveFormat getArchiveFormat() {
        return ArchiveFormat.CSV;
    }
    
    // === Private Methods ===
    
    private Path getCsvPath(LocalDate date) {
        var archiveBasePath = policyProvider.getCurrentPolicy()
            .getArchivingStrategy()
            .getArchiveBasePath();
        
        var dateDir = date.format(DATE_FORMATTER);
        var fileName = dateDir + ".csv";
        
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
            log.error("CSV 데이터 압축 실패. 파일: {}", originalPath, e);
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
            log.error("CSV 데이터 압축 해제 실패. 파일: {}", filePath, e);
            return data; // 압축 해제 실패 시 원본 데이터 반환
        }
    }
    
    private void createDirectoryIfNotExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
    }
    
    private String generateCsvContent(List<AuthLog> authLogs) {
        var content = new StringBuilder();
        content.append(CSV_HEADER).append("\n");
        
        for (var authLog : authLogs) {
            var line = new StringJoiner(CSV_DELIMITER);
            line.add(String.valueOf(authLog.getId()));
            line.add(escapeCsvValue(authLog.getDate().toString()));
            line.add(escapeCsvValue(authLog.getDevice()));
            line.add(escapeCsvValue(authLog.getUserId()));
            line.add(escapeCsvValue(authLog.getResult()));
            line.add(escapeCsvValue(authLog.getEndpoint()));
            
            content.append(line.toString()).append("\n");
        }
        
        return content.toString();
    }
    
    private List<AuthLog> parseCsvContent(String csvContent) throws IOException {
        var authLogs = new ArrayList<AuthLog>();
        
        try (var reader = new BufferedReader(new StringReader(csvContent))) {
            // 헤더 스킵
            var header = reader.readLine();
            if (header == null || !header.equals(CSV_HEADER)) {
                log.warn("CSV 헤더가 예상과 다릅니다. 헤더: {}", header);
            }
            
            String line;
            var lineNumber = 2; // 헤더 다음부터
            
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue; // 빈 줄 스킵
                }
                
                try {
                    var authLog = parseCsvLine(line);
                    authLogs.add(authLog);
                } catch (Exception e) {
                    log.error("CSV 라인 파싱 실패. 라인 {}: {}", lineNumber, line, e);
                    // 개별 라인 파싱 실패는 전체 실패로 이어지지 않음
                }
                
                lineNumber++;
            }
        }
        
        return authLogs;
    }
    
    private AuthLog parseCsvLine(String line) {
        var parts = parseCsvValues(line);
        
        if (parts.length != 6) {
            throw new IllegalArgumentException("CSV 라인의 컬럼 수가 올바르지 않습니다. 예상: 6, 실제: " + parts.length);
        }
        
        return AuthLog.of(
            Long.parseLong(parts[0]),
            Instant.parse(parts[1]),
            parts[2],
            parts[3],
            parts[4],
            parts[5]
        );
    }
    
    private String[] parseCsvValues(String line) {
        var values = new ArrayList<String>();
        var current = new StringBuilder();
        var inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // 이스케이프된 따옴표
                    current.append('"');
                    i++; // 다음 따옴표 스킵
                } else {
                    // 따옴표 시작/끝
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // 구분자
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        
        // 마지막 값 추가
        values.add(current.toString());
        
        return values.toArray(new String[0]);
    }
    
    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }
        
        // 따옴표, 쉼표, 개행이 포함된 경우 따옴표로 감싸고 따옴표를 이스케이프
        if (value.contains("\"") || value.contains(",") || value.contains("\n") || value.contains("\r")) {
            return CSV_QUOTE + value.replace("\"", "\"\"") + CSV_QUOTE;
        }
        
        return value;
    }
}