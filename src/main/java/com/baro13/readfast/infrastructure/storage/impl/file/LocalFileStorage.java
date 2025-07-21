package com.baro13.readfast.infrastructure.storage.impl.file;

import com.baro13.readfast.domain.AuthLog;
import com.baro13.readfast.infrastructure.policy.DataRetentionProperties;
import com.baro13.readfast.infrastructure.storage.port.DataStorage;
import com.baro13.readfast.infrastructure.storage.port.StorageType;
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
                log.debug("파일이 존재하지 않음: {}", filePath);
                return Collections.emptyList();
            }
            
            if (properties.isEnableCompression() && "gzip".equals(properties.getCompressionFormat())) {
                return retrieveCompressed(filePath);
            } else {
                return retrieveUncompressed(filePath);
            }
        } catch (IOException e) {
            log.error("{}일자 인증 로그 조회 실패", date, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<AuthLog> retrieveByDateRange(LocalDate startDate, LocalDate endDate) {
        List<AuthLog> results = new ArrayList<>();
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            results.addAll(retrieve(current));
            current = current.plusDays(1);
        }
        
        return results.stream()
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
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
                log.info("파일 삭제 완료: {}", filePath);
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

    private void storeCompressed(List<AuthLog> authLogs, Path filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile());
             GZIPOutputStream gzipOut = new GZIPOutputStream(fos);
             OutputStreamWriter writer = new OutputStreamWriter(gzipOut)) {
            
            if ("csv".equals(properties.getArchiveDataFormat())) {
                writeCsv(authLogs, writer);
            } else {
                writeJson(authLogs, writer);
            }
        }
    }

    private void storeUncompressed(List<AuthLog> authLogs, Path filePath) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(filePath.toFile()))) {
            if ("csv".equals(properties.getArchiveDataFormat())) {
                writeCsv(authLogs, writer);
            } else {
                writeJson(authLogs, writer);
            }
        }
    }

    private List<AuthLog> retrieveCompressed(Path filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             GZIPInputStream gzipIn = new GZIPInputStream(fis);
             InputStreamReader reader = new InputStreamReader(gzipIn)) {
            
            if ("csv".equals(properties.getArchiveDataFormat())) {
                return readCsv(reader);
            } else {
                return readJson(reader);
            }
        }
    }

    private List<AuthLog> retrieveUncompressed(Path filePath) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(filePath.toFile()))) {
            if ("csv".equals(properties.getArchiveDataFormat())) {
                return readCsv(reader);
            } else {
                return readJson(reader);
            }
        }
    }

    private void writeJson(List<AuthLog> authLogs, OutputStreamWriter writer) throws IOException {
        objectMapper.writeValue(writer, authLogs);
    }

    private void writeCsv(List<AuthLog> authLogs, OutputStreamWriter writer) throws IOException {
        // CSV 헤더
        writer.write("date,device,userId,result,endpoint\n");
        
        for (AuthLog log : authLogs) {
            writer.write(String.format("%s,%s,%s,%s,%s\n",
                log.getDate().toString(),
                escapeCSV(log.getDevice()),
                escapeCSV(log.getUserId()),
                escapeCSV(log.getResult()),
                escapeCSV(log.getEndpoint())
            ));
        }
    }

    private List<AuthLog> readJson(InputStreamReader reader) throws IOException {
        AuthLog[] authLogs = objectMapper.readValue(reader, AuthLog[].class);
        return Arrays.asList(authLogs);
    }

    private List<AuthLog> readCsv(InputStreamReader reader) throws IOException {
        // CSV 파싱 로직 (간단 구현)
        List<AuthLog> results = new ArrayList<>();
        // 실제로는 OpenCSV 등의 라이브러리 사용 권장
        log.warn("CSV 읽기 기능은 아직 구현되지 않았습니다.");
        return results;
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
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

    private void createDirectoryIfNotExists(Path directory) throws IOException {
        if (directory != null && !Files.exists(directory)) {
            Files.createDirectories(directory);
        }
    }
}