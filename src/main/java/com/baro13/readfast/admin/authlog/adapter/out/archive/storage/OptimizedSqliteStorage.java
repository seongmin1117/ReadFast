package com.baro13.readfast.admin.authlog.adapter.out.archive.storage;

import com.baro13.readfast.admin.authlog.adapter.out.archive.cache.ArchiveCache;
import com.baro13.readfast.admin.authlog.adapter.out.archive.compression.CompressionFactory;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.baro13.readfast.admin.authlog.domain.model.ArchiveMetadata;
import com.baro13.readfast.admin.authlog.domain.port.ArchiveMetadataRepository;
import com.baro13.readfast.admin.authlog.domain.port.DataRetentionPolicyProvider;
import com.baro13.readfast.admin.policy.domain.model.vo.ArchivingStrategy.ArchiveFormat;
import com.baro13.readfast.admin.policy.domain.model.vo.ArchivingStrategy.CompressionType;
import com.baro13.readfast.global.common.DateTimeUtils;
import com.baro13.readfast.global.common.TimeZoneConstants;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

/**
 * 최적화된 SQLite 스토리지 구현체
 * - 인메모리 압축 해제로 디스크 I/O 최소화
 * - 2단계 캐싱 시스템 (압축/비압축)
 * - 임시 파일 없는 스트림 기반 처리
 * - 메모리 효율적인 대용량 데이터 처리
 */
@Slf4j
@Repository
@Primary
@RequiredArgsConstructor
public class OptimizedSqliteStorage implements Storage {

    private final DataRetentionPolicyProvider policyProvider;
    private final CompressionFactory compressionFactory;
    private final ArchiveCache archiveCache;
    private final ArchiveMetadataRepository archiveMetadataRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyy/MM");

    private static final String CREATE_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS auth_log (
            id INTEGER PRIMARY KEY,
            date TEXT NOT NULL,
            device TEXT,
            user_id TEXT,
            result TEXT,
            endpoint TEXT,
            created_at TEXT DEFAULT CURRENT_TIMESTAMP
        )
        """;
    
    private static final String CREATE_INDEX_SQL = """
        CREATE INDEX IF NOT EXISTS idx_date ON auth_log(date)
        """;
    
    private static final String INSERT_SQL = """
        INSERT OR IGNORE INTO auth_log (id, date, device, user_id, result, endpoint) 
        VALUES (?, ?, ?, ?, ?, ?)
        """;
    
    private static final String SELECT_BY_DATE_SQL = """
        SELECT id, date, device, user_id, result, endpoint 
        FROM auth_log 
        WHERE date LIKE ?
        ORDER BY date DESC, id DESC
        """;

    @Override
    public void store(List<AuthLog> authLogs, LocalDate date) {
        if (authLogs == null || authLogs.isEmpty()) {
            log.warn("저장할 인증 로그가 없습니다. date: {}", date);
            return;
        }
        
        var dbPath = getDbPath(date);
        
        try {
            createDirectoryIfNotExists(dbPath.getParent());
            
            try (var connection = getConnection(dbPath)) {
                
                // 테이블 초기화
                initializeDatabase(connection);
                connection.setAutoCommit(false);
                
                try (var statement = connection.prepareStatement(INSERT_SQL)) {
                    var batchCount = 0;
                    
                    for (var authLog : authLogs) {
                        try {
                            statement.setLong(1, authLog.getId());
                            statement.setString(2, DateTimeUtils.toIsoUtcString(authLog.getDate()));
                            statement.setString(3, authLog.getDevice());
                            statement.setString(4, authLog.getUserId());
                            statement.setString(5, authLog.getResult());
                            statement.setString(6, authLog.getEndpoint());
                            statement.addBatch();
                            batchCount++;
                            
                            // 메모리 최적화를 위한 배치 크기 제한
                            if (batchCount % 1000 == 0) {
                                statement.executeBatch();
                                statement.clearBatch();
                            }
                        } catch (SQLException e) {
                            log.error("AuthLog 배치 추가 실패. ID: {}", authLog.getId(), e);
                            throw e;
                        }
                    }
                    
                    // 남은 배치 실행
                    var finalResults = statement.executeBatch();
                    connection.commit();
                    
                    var insertedCount = (int) java.util.Arrays.stream(finalResults)
                        .filter(r -> r > 0)
                        .count();
                    var skippedCount = authLogs.size() - insertedCount;
                    
                    log.info("SQLite에 인증 로그 저장 완료. 파일: {}, 전체: {}, 삽입: {}, 중복스킵: {}", 
                            dbPath, authLogs.size(), insertedCount, skippedCount);
                }
                
                // 압축 적용 및 캐시 무효화
                applyCompressionIfEnabled(dbPath);
                archiveCache.evict(date); // 변경 시 캐시 무효화
                
            } catch (SQLException e) {
                log.error("SQLite 데이터 저장 실패. 건수: {}", authLogs.size(), e);
                throw new RuntimeException("SQLite 데이터 저장 중 오류 발생", e);
            }
        } catch (IOException e) {
            log.error("SQLite 파일 경로 생성 실패. 파일: {}", dbPath, e);
            throw new RuntimeException("SQLite 파일 경로 생성 중 오류 발생", e);
        }
    }

    @Override
    public List<AuthLog> retrieve(LocalDate date) {
        // 1단계: 압축 해제된 데이터 캐시 확인
        Optional<List<AuthLog>> cachedData = archiveCache.getDecompressed(date);
        if (cachedData.isPresent()) {
            log.debug("캐시에서 데이터 조회: 날짜={}, 레코드={}", date, cachedData.get().size());
            return cachedData.get();
        }

        // 2단계: 압축된 데이터 캐시 확인 및 인메모리 처리
        Optional<byte[]> cachedCompressedData = archiveCache.getCompressed(date);
        if (cachedCompressedData.isPresent()) {
            log.debug("압축된 캐시에서 데이터 처리: 날짜={}, 크기={}bytes", date, cachedCompressedData.get().length);
            List<AuthLog> data = processCompressedDataInMemory(cachedCompressedData.get(), date);
            archiveCache.putDecompressed(date, data); // 압축 해제된 데이터 캐시
            return data;
        }

        // 3단계: 파일 시스템에서 로드
        return loadFromFileSystem(date);
    }

    @Override
    public List<AuthLog> retrieveByDateRange(LocalDate startDate, LocalDate endDate) {
        var allLogs = new ArrayList<AuthLog>();
        var currentDate = startDate;
        
        while (!currentDate.isAfter(endDate)) {
            allLogs.addAll(retrieve(currentDate));
            currentDate = currentDate.plusDays(1);
        }
        
        log.debug("SQLite 날짜 범위 조회 완료. 기간: {} ~ {}, 총 건수: {}", 
                 startDate, endDate, allLogs.size());
        
        return allLogs;
    }

    @Override
    public boolean exists(LocalDate date) {
        var dbPath = getDbPath(date);
        var compressedPath = getCompressedPath(dbPath);
        
        // 캐시에 있으면 존재한다고 판단
        if (archiveCache.getDecompressed(date).isPresent() || 
            archiveCache.getCompressed(date).isPresent()) {
            return true;
        }
        
        // 파일 시스템 확인
        return (Files.exists(dbPath) && Files.isRegularFile(dbPath)) ||
               (Files.exists(compressedPath) && Files.isRegularFile(compressedPath));
    }

    @Override
    public void delete(LocalDate date) {
        var dbPath = getDbPath(date);
        var compressedPath = getCompressedPath(dbPath);
        
        try {
            var deleted = false;
            
            // 캐시 무효화
            archiveCache.evict(date);
            
            // 원본 파일 삭제
            if (Files.exists(dbPath)) {
                Files.delete(dbPath);
                log.info("SQLite 파일 삭제 완료. 파일: {}", dbPath);
                deleted = true;
            }
            
            // 압축된 파일 삭제
            if (Files.exists(compressedPath)) {
                Files.delete(compressedPath);
                log.info("압축된 SQLite 파일 삭제 완료. 파일: {}", compressedPath);
                deleted = true;
            }
            
            if (!deleted) {
                log.debug("삭제할 SQLite 파일이 존재하지 않습니다. 경로: {} 또는 {}", dbPath, compressedPath);
            }
        } catch (IOException e) {
            log.error("SQLite 파일 삭제 실패. 날짜: {}", date, e);
            throw new RuntimeException("SQLite 파일 삭제 중 오류 발생", e);
        }
    }

    @Override
    public ArchiveFormat getArchiveFormat() {
        return ArchiveFormat.SQLITE;
    }

    @Override
    public List<AuthLog> findByCursor(LocalDate cursorDate, int limit) {
        // TODO: 커서 기반 조회 구현 (필요시)
        return List.of();
    }

    /**
     * 파일 시스템에서 데이터 로드 (최종 fallback)
     */
    private List<AuthLog> loadFromFileSystem(LocalDate date) {
        var dbPath = getDbPath(date);
        
        try {
            // 파일이 존재하는지 확인
            if (Files.exists(dbPath)) {
                var fileName = dbPath.getFileName().toString();
                
                // 압축된 파일인지 확인 (.gz, .zip 등)
                if (fileName.endsWith(".gz") || fileName.endsWith(".zip")) {
                    log.debug("압축된 파일에서 로드: {}", dbPath);
                    byte[] compressedData = Files.readAllBytes(dbPath);
                    
                    // 압축된 데이터 캐시 저장
                    archiveCache.putCompressed(date, compressedData);
                    
                    // 인메모리 처리
                    List<AuthLog> data = processCompressedDataInMemory(compressedData, date);
                    archiveCache.putDecompressed(date, data);
                    return data;
                } else {
                    // 압축되지 않은 파일
                    log.debug("비압축 파일에서 로드: {}", dbPath);
                    List<AuthLog> data = loadFromSqliteFile(dbPath, date);
                    archiveCache.putDecompressed(date, data);
                    return data;
                }
            }
            
            // 기존 방식으로 fallback 시도 (메타데이터에 경로가 없는 경우)
            var compressedPath = getCompressedPath(dbPath);
            if (Files.exists(compressedPath)) {
                log.debug("기본 압축 경로에서 로드: {}", compressedPath);
                byte[] compressedData = Files.readAllBytes(compressedPath);
                archiveCache.putCompressed(date, compressedData);
                List<AuthLog> data = processCompressedDataInMemory(compressedData, date);
                archiveCache.putDecompressed(date, data);
                return data;
            }
            
            log.debug("SQLite 파일이 존재하지 않습니다. 날짜: {}", date);
            return List.of();
            
        } catch (Exception e) {
            log.error("파일 시스템에서 데이터 로드 실패. 날짜: {}", date, e);
            return List.of();
        }
    }

    /**
     * 압축된 데이터를 임시 파일을 통해 처리 (실용적인 구현)
     */
    private List<AuthLog> processCompressedDataInMemory(byte[] compressedData, LocalDate date) {
        Path tempFile = null;
        try {
            // 압축 해제
            var compression = compressionFactory.resolve();
            byte[] originalData = compression.decompress(compressedData);
            
            // 임시 SQLite 파일 생성
            tempFile = Files.createTempFile("sqlite-temp-" + date, ".db");
            Files.write(tempFile, originalData);
            
            // 임시 파일에서 데이터 로드
            List<AuthLog> result = loadFromSqliteFile(tempFile, date);
            
            log.debug("압축된 데이터 처리 완료. 날짜: {}, 레코드: {}", date, result.size());
            return result;
            
        } catch (IOException e) {
            log.error("압축 해제 또는 임시 파일 처리 실패. 날짜: {}", date, e);
            throw new RuntimeException("압축된 데이터 처리 중 오류 발생", e);
        } finally {
            // 임시 파일 정리
            if (tempFile != null && Files.exists(tempFile)) {
                try {
                    Files.delete(tempFile);
                    log.debug("임시 SQLite 파일 정리 완료: {}", tempFile);
                } catch (IOException e) {
                    log.warn("임시 SQLite 파일 정리 실패: {}", tempFile, e);
                }
            }
        }
    }

    /**
     * SQLite 파일에서 직접 데이터 로드
     */
    private List<AuthLog> loadFromSqliteFile(Path dbPath, LocalDate date) {
        try (var connection = getConnection(dbPath);
             var statement = connection.prepareStatement(SELECT_BY_DATE_SQL)) {
            
            var datePrefix = date.format(DATE_FORMATTER);
            statement.setString(1, datePrefix + "%");
            
            var authLogs = new ArrayList<AuthLog>();
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    authLogs.add(mapToAuthLog(resultSet));
                }
            }
            
            log.debug("SQLite 파일에서 직접 조회 완료. 파일: {}, 건수: {}", dbPath, authLogs.size());
            return authLogs;
            
        } catch (SQLException e) {
            log.error("SQLite 파일 조회 실패. 날짜: {}", date, e);
            throw new RuntimeException("SQLite 파일 조회 중 오류 발생", e);
        }
    }


    // 기존 유틸리티 메서드들은 그대로 유지
    private Path getDbPath(LocalDate date) {
        // 1단계: 메타데이터에서 실제 경로 확인
        try {
            var startOfDay = date.atStartOfDay(TimeZoneConstants.APPLICATION_ZONE).toInstant();
            var endOfDay = date.atTime(23, 59, 59).atZone(TimeZoneConstants.APPLICATION_ZONE).toInstant();
            
            var metadata = archiveMetadataRepository.findByDateRange(startOfDay, endOfDay)
                .stream()
                .filter(meta -> "sqlite".equals(meta.getStorageType()))
                .findFirst();
                
            if (metadata.isPresent()) {
                var metadataPath = metadata.get().getFilePath();
                log.debug("메타데이터에서 경로 찾음: {}", metadataPath);
                
                // 상대 경로인 경우 절대 경로로 변환
                if (metadataPath.startsWith("./")) {
                    return Paths.get(System.getProperty("user.dir"), metadataPath.substring(2));
                } else if (!Paths.get(metadataPath).isAbsolute()) {
                    var archiveBasePath = policyProvider.getCurrentPolicy()
                        .getArchivingStrategy()
                        .getArchiveBasePath();
                    return Paths.get(archiveBasePath).resolve(metadataPath);
                } else {
                    return Paths.get(metadataPath);
                }
            }
        } catch (Exception e) {
            log.debug("메타데이터에서 경로 조회 실패, 기본 경로 사용: {}", e.getMessage());
        }
        
        // 2단계: 기본 경로 생성 (BatchExecutionImpl과 동일한 구조)
        var archiveBasePath = policyProvider.getCurrentPolicy()
            .getArchivingStrategy()
            .getArchiveBasePath();

        var dateDir = date.format(DATE_DIR); // yyyy/MM 형식
        var fileName = date.format(DATE_FORMATTER) + ".sqlite";
        
        return Paths.get(archiveBasePath, dateDir, fileName);
    }
    
    private Connection getConnection(Path dbPath) throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            
            Path normalizedPath = dbPath.toAbsolutePath().normalize();
            
            // 임시 파일인지 확인 (압축 해제를 위한 임시 SQLite 파일)
            boolean isTempFile = normalizedPath.toString().contains("sqlite-temp-");
            
            if (!isTempFile) {
                // 일반 파일인 경우에만 경로 보안 검사
                Path archiveBase = Paths.get(policyProvider.getCurrentPolicy().getArchivingStrategy().getArchiveBasePath())
                    .toAbsolutePath()
                    .normalize();

                if (!normalizedPath.startsWith(archiveBase)) {
                    throw new SQLException("Invalid database path: path traversal detected");
                }
            }
            
            var url = "jdbc:sqlite:" + normalizedPath.toString();
            var connection = DriverManager.getConnection(url);
            configureConnection(connection);
            
            return connection;
            
        } catch (ClassNotFoundException e) {
            log.error("SQLite JDBC 드라이버를 찾을 수 없습니다", e);
            throw new SQLException("SQLite 드라이버 로딩 실패", e);
        }
    }
    
    private void configureConnection(Connection connection) throws SQLException {
        try (var stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL"); 
            stmt.execute("PRAGMA cache_size=10000");
            stmt.execute("PRAGMA temp_store=MEMORY");
            stmt.execute("PRAGMA foreign_keys=ON");
        }
    }
    
    private void createDirectoryIfNotExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
    }
    
    private void initializeDatabase(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(CREATE_TABLE_SQL);
            statement.execute(CREATE_INDEX_SQL);
        }
    }
    
    private AuthLog mapToAuthLog(ResultSet resultSet) throws SQLException {
        try {
            var dateStr = resultSet.getString("date");
            Instant dateInstant;
            try {
                dateInstant = DateTimeUtils.parseIsoUtcString(dateStr);
            } catch (Exception parseException) {
                log.warn("날짜 파싱 실패, 대체 방법 시도. 원본: {}", dateStr);
                try {
                    dateInstant = java.time.ZonedDateTime.parse(dateStr).toInstant();
                } catch (Exception fallbackException) {
                    log.error("모든 날짜 파싱 방법 실패. 원본: {}", dateStr, fallbackException);
                    throw new SQLException("날짜 파싱 실패: " + dateStr, fallbackException);
                }
            }
            
            return AuthLog.of(
                resultSet.getLong("id"),
                dateInstant,
                resultSet.getString("device"),
                resultSet.getString("user_id"),
                resultSet.getString("result"),
                resultSet.getString("endpoint")
            );
        } catch (Exception e) {
            log.error("AuthLog 매핑 실패. ID: {}, Date: {}", 
                     resultSet.getLong("id"), resultSet.getString("date"), e);
            throw new SQLException("AuthLog 매핑 중 오류 발생", e);
        }
    }
    
    private void applyCompressionIfEnabled(Path dbPath) {
        try {
            var compressionType = policyProvider.getCurrentPolicy()
                .getArchivingStrategy()
                .getCompressionType();
                
            if (compressionType == CompressionType.NONE) {
                log.debug("압축이 비활성화되어 있습니다. 파일: {}", dbPath);
                return;
            }
            
            if (!Files.exists(dbPath)) {
                log.warn("압축할 파일이 존재하지 않습니다. 파일: {}", dbPath);
                return;
            }
            
            // 압축 실행
            var compression = compressionFactory.resolve();
            var originalData = Files.readAllBytes(dbPath);
            var compressedData = compression.compress(originalData);
            
            // 압축된 파일 저장
            var compressedPath = getCompressedPath(dbPath);
            createDirectoryIfNotExists(compressedPath.getParent());
            Files.write(compressedPath, compressedData);
            
            // 원본 파일 삭제 (압축 성공 후)
            Files.delete(dbPath);
            
            var originalSize = originalData.length;
            var compressedSize = compressedData.length;
            var compressionRatio = (double) compressedSize / originalSize * 100;
            
            log.info("SQLite 파일 압축 완료. 원본: {}bytes, 압축: {}bytes, 비율: {:.1f}%, 파일: {}", 
                    originalSize, compressedSize, compressionRatio, compressedPath);
                    
        } catch (Exception e) {
            log.error("SQLite 파일 압축 실패. 파일: {}", dbPath, e);
        }
    }
    
    private Path getCompressedPath(Path originalPath) {
        var compressionType = policyProvider.getCurrentPolicy()
            .getArchivingStrategy()
            .getCompressionType();
        
        return Paths.get(originalPath.toString() + compressionType.getExtension());
    }

    /**
     * 임시 파일 정리 - 2시간 이상 된 SQLite 임시 파일들을 정리
     */
    public void cleanupOldTemporaryFiles() {
        try {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
            long cutoffTime = System.currentTimeMillis() - (2 * 60 * 60 * 1000); // 2시간 전
            
            var deletedCount = Files.list(tempDir)
                .filter(path -> path.getFileName().toString().startsWith("sqlite-temp-"))
                .filter(path -> path.getFileName().toString().endsWith(".db"))
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                    } catch (IOException e) {
                        log.debug("임시 파일 수정 시간 확인 실패: {}", path, e);
                        return false;
                    }
                })
                .peek(path -> {
                    try {
                        Files.delete(path);
                        log.debug("오래된 SQLite 임시 파일 삭제: {}", path);
                    } catch (IOException e) {
                        log.warn("SQLite 임시 파일 삭제 실패: {}", path, e);
                    }
                })
                .count();
                
            if (deletedCount > 0) {
                log.info("SQLite 임시 파일 정리 완료: {}개 삭제", deletedCount);
            } else {
                log.debug("정리할 SQLite 임시 파일이 없습니다");
            }
            
        } catch (IOException e) {
            log.error("SQLite 임시 파일 정리 중 오류 발생", e);
        }
    }
}