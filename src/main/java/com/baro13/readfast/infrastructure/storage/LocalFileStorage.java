package com.baro13.readfast.infrastructure.storage;

import com.baro13.readfast.domain.AuthLog;
import com.baro13.readfast.infrastructure.batch.config.DataRetentionProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalFileStorage implements DataStorage {
    
    private final DataRetentionProperties properties;
    private final ObjectMapper objectMapper;
    
    @Override
    public void store(List<AuthLog> authLogs, LocalDate date) {
        try {
            Path filePath = getFilePath(date);
            createDirectoryIfNotExists(filePath.getParent());
            
            if (properties.isEnableCompression() && "gzip".equals(properties.getCompressionFormat())) {
                storeCompressed(authLogs, filePath);
            } else {
                storeUncompressed(authLogs, filePath);
            }
            
            log.info("{}개 레코드를 파일에 성공적으로 저장: {}", authLogs.size(), filePath);
        } catch (IOException e) {
            log.error("{}일자 인증 로그 저장 실패", date, e);
            throw new RuntimeException("인증 로그 저장 실패", e);
        }
    }
    
    @Override
    public List<AuthLog> retrieve(LocalDate date) {
        try {
            Path filePath = getFilePath(date);
            if (!Files.exists(filePath)) {
                return Collections.emptyList();
            }
            
            if (properties.isEnableCompression() && "gzip".equals(properties.getCompressionFormat())) {
                return retrieveCompressed(filePath);
            } else {
                return retrieveUncompressed(filePath);
            }
        } catch (IOException e) {
            log.error("{}일자 인증 로그 조회 실패", date, e);
            throw new RuntimeException("인증 로그 조회 실패", e);
        }
    }
    
    @Override
    public List<AuthLog> retrieveByDateRange(LocalDate startDate, LocalDate endDate) {
        return startDate.datesUntil(endDate.plusDays(1))
                .flatMap(date -> retrieve(date).stream())
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean exists(LocalDate date) {
        Path filePath = getFilePath(date);
        return Files.exists(filePath);
    }
    
    @Override
    public void delete(LocalDate date) {
        try {
            Path filePath = getFilePath(date);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("파일 삭제 성공: {}", filePath);
            }
        } catch (IOException e) {
            log.error("{}일자 파일 삭제 실패", date, e);
            throw new RuntimeException("파일 삭제 실패", e);
        }
    }
    
    @Override
    public StorageType getStorageType() {
        return StorageType.LOCAL_FILE;
    }
    
    private Path getFilePath(LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.ofPattern(properties.getArchiveFileFormat()));
        String extension = getFileExtension();
        String fileName = dateStr + extension;
        return Paths.get(properties.getArchiveBasePath(), fileName);
    }
    
    private String getFileExtension() {
        String dataFormat = properties.getArchiveDataFormat();
        boolean isCompressed = properties.isEnableCompression() && "gzip".equals(properties.getCompressionFormat());
        
        if ("csv".equals(dataFormat)) {
            return isCompressed ? ".csv.gz" : ".csv";
        } else if ("json".equals(dataFormat)) {
            return isCompressed ? ".json.gz" : ".json";
        }
        return isCompressed ? ".json.gz" : ".json";
    }
    
    private void storeCompressed(List<AuthLog> authLogs, Path filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile());
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             OutputStreamWriter writer = new OutputStreamWriter(gzos)) {
            
            if ("csv".equals(properties.getArchiveDataFormat())) {
                writeCsvData(authLogs, writer);
            } else {
                writeJsonData(authLogs, writer);
            }
        }
    }
    
    private void storeUncompressed(List<AuthLog> authLogs, Path filePath) throws IOException {
        if ("csv".equals(properties.getArchiveDataFormat())) {
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(filePath.toFile()))) {
                writeCsvData(authLogs, writer);
            }
        } else {
            objectMapper.writeValue(filePath.toFile(), authLogs);
        }
    }
    
    private List<AuthLog> retrieveCompressed(Path filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             GZIPInputStream gzis = new GZIPInputStream(fis);
             InputStreamReader reader = new InputStreamReader(gzis)) {
            
            if ("csv".equals(properties.getArchiveDataFormat())) {
                return readCsvData(reader);
            } else {
                return readJsonData(reader);
            }
        }
    }
    
    private List<AuthLog> retrieveUncompressed(Path filePath) throws IOException {
        if ("csv".equals(properties.getArchiveDataFormat())) {
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(filePath.toFile()))) {
                return readCsvData(reader);
            }
        } else {
            AuthLog[] authLogs = objectMapper.readValue(filePath.toFile(), AuthLog[].class);
            return Arrays.asList(authLogs);
        }
    }
    
    private void writeCsvData(List<AuthLog> authLogs, OutputStreamWriter writer) throws IOException {
        // CSV 헤더 작성
        writer.write("id,date,device,userId,result,endpoint\n");
        
        // 데이터 작성
        for (AuthLog authLog : authLogs) {
            writer.write(String.format("%s,%s,%s,%s,%s,%s\n",
                escapeCSV(authLog.getId().toString()),
                escapeCSV(authLog.getDate().toString()),
                escapeCSV(authLog.getDevice()),
                escapeCSV(authLog.getUserId()),
                escapeCSV(authLog.getResult()),
                escapeCSV(authLog.getEndpoint())
            ));
        }
    }
    
    private void writeJsonData(List<AuthLog> authLogs, OutputStreamWriter writer) throws IOException {
        writer.write(objectMapper.writeValueAsString(authLogs));
    }
    
    private List<AuthLog> readCsvData(InputStreamReader reader) throws IOException {
        List<AuthLog> authLogs = new ArrayList<>();
        try (java.io.BufferedReader br = new java.io.BufferedReader(reader)) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false; // 헤더 건너뛰기
                    continue;
                }
                
                String[] fields = line.split(",");
                if (fields.length >= 6) {
                    AuthLog authLog = AuthLog.builder()
                        .id(Long.parseLong(fields[0]))
                        .date(java.time.Instant.parse(fields[1]))
                        .device(fields[2])
                        .userId(fields[3])
                        .result(fields[4])
                        .endpoint(fields[5])
                        .build();
                    authLogs.add(authLog);
                }
            }
        }
        return authLogs;
    }
    
    private List<AuthLog> readJsonData(InputStreamReader reader) throws IOException {
        AuthLog[] authLogs = objectMapper.readValue(reader, AuthLog[].class);
        return Arrays.asList(authLogs);
    }
    
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    private void createDirectoryIfNotExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
            log.info("디렉토리 생성: {}", directory);
        }
    }
}