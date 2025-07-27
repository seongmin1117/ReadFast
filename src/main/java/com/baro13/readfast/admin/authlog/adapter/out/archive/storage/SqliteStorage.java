package com.baro13.readfast.admin.authlog.adapter.out.archive.storage;

import com.baro13.readfast.admin.authlog.adapter.out.archive.compression.CompressionFactory;
import com.baro13.readfast.admin.authlog.domain.model.AuthLog;
import com.baro13.readfast.admin.authlog.domain.port.DataRetentionPolicyProvider;
import com.baro13.readfast.admin.policy.domain.model.vo.ArchivingStrategy.ArchiveFormat;
import com.baro13.readfast.admin.policy.domain.model.vo.ArchivingStrategy.CompressionType;
import com.baro13.readfast.global.common.TimeZoneConstants;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SqliteStorage implements Storage {

    private final DataRetentionPolicyProvider policyProvider;
    private final CompressionFactory compressionFactory;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
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
                
                // 먼저 테이블을 생성하고 auto commit 설정
                initializeDatabase(connection);
                connection.setAutoCommit(false);
                
                try (var statement = connection.prepareStatement(INSERT_SQL)) {
                    var batchCount = 0;
                    
                    for (var authLog : authLogs) {
                        try {
                            statement.setLong(1, authLog.getId());
                            statement.setString(2, authLog.getDate().atZone(TimeZoneConstants.APPLICATION_ZONE).toString());
                            statement.setString(3, authLog.getDevice());
                            statement.setString(4, authLog.getUserId());
                            statement.setString(5, authLog.getResult());
                            statement.setString(6, authLog.getEndpoint());
                            statement.addBatch();
                            batchCount++;
                            
                            // 배치 크기 제한으로 메모리 사용량 최적화
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
                
                // 압축 적용 (정책에 따라)
                applyCompressionIfEnabled(dbPath);
                
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
        var dbPath = getDbPath(date);
        
        // 압축 해제 (필요한 경우)
        var actualDbPath = decompressIfNeeded(dbPath);
        
        if (!Files.exists(actualDbPath)) {
            log.debug("SQLite 파일이 존재하지 않습니다. 파일: {}", actualDbPath);
            return List.of();
        }
        
        var datePrefix = date.format(DATE_FORMATTER);
        var authLogs = new ArrayList<AuthLog>();
        
        try (var connection = getConnection(actualDbPath);
             var statement = connection.prepareStatement(SELECT_BY_DATE_SQL)) {
            
            statement.setString(1, datePrefix + "%");
            
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    authLogs.add(mapToAuthLog(resultSet));
                }
            }
            
            log.debug("SQLite에서 인증 로그 조회 완료. 파일: {}, 건수: {}", actualDbPath, authLogs.size());
            return authLogs;
            
        } catch (SQLException e) {
            log.error("SQLite 데이터 조회 실패. 날짜: {}", date, e);
            throw new RuntimeException("SQLite 데이터 조회 중 오류 발생", e);
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
        
        log.debug("SQLite 날짜 범위 조회 완료. 기간: {} ~ {}, 총 건수: {}", 
                 startDate, endDate, allLogs.size());
        
        return allLogs;
    }

    @Override
    public boolean exists(LocalDate date) {
        var dbPath = getDbPath(date);
        var compressedPath = getCompressedPath(dbPath);
        
        // 원본 파일 또는 압축된 파일이 존재하는지 확인
        return (Files.exists(dbPath) && Files.isRegularFile(dbPath)) ||
               (Files.exists(compressedPath) && Files.isRegularFile(compressedPath));
    }

    @Override
    public void delete(LocalDate date) {
        var dbPath = getDbPath(date);
        var compressedPath = getCompressedPath(dbPath);
        
        try {
            var deleted = false;
            
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
    
    private Path getDbPath(LocalDate date) {
        var archiveBasePath = policyProvider.getCurrentPolicy()
            .getArchivingStrategy()
            .getArchiveBasePath();
        
        var dateDir = date.format(DATE_FORMATTER);
        var fileName = dateDir + ".sqlite";
        
        return Paths.get(archiveBasePath, dateDir, fileName);
    }
    
    private Connection getConnection(Path dbPath) throws SQLException {
        try {
            // SQLite 드라이버 명시적 로딩
            Class.forName("org.sqlite.JDBC");
            
            // 경로 검증 및 정규화
            var normalizedPath = dbPath.toAbsolutePath().normalize();
            if (!normalizedPath.toString().contains(policyProvider.getCurrentPolicy().getArchivingStrategy().getArchiveBasePath())) {
                throw new SQLException("Invalid database path: path traversal detected");
            }
            
            var url = "jdbc:sqlite:" + normalizedPath.toString();
            log.debug("SQLite 연결 초기화");
            
            var connection = DriverManager.getConnection(url);
            configureConnection(connection);
            
            return connection;
            
        } catch (ClassNotFoundException e) {
            log.error("SQLite JDBC 드라이버를 찾을 수 없습니다", e);
            throw new SQLException("SQLite 드라이버 로딩 실패", e);
        } catch (SQLException e) {
            log.error("SQLite 연결 실패", e);
            throw e;
        }
    }
    
    private void configureConnection(Connection connection) throws SQLException {
        // SQLite 최적화 설정
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
            return AuthLog.of(
                resultSet.getLong("id"),
                Instant.parse(resultSet.getString("date")),
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
    
    /**
     * 압축 적용 (정책에 따라)
     */
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
            // 압축 실패해도 원본 파일은 유지 (데이터 보호)
        }
    }
    
    /**
     * 압축 해제 (필요한 경우)
     */
    private Path decompressIfNeeded(Path dbPath) {
        try {
            var compressionType = policyProvider.getCurrentPolicy()
                .getArchivingStrategy()
                .getCompressionType();
                
            if (compressionType == CompressionType.NONE) {
                return dbPath; // 압축 사용하지 않음
            }
            
            var compressedPath = getCompressedPath(dbPath);
            
            // 압축된 파일이 존재하는 경우
            if (Files.exists(compressedPath)) {
                // 원본 파일이 이미 존재하면 그대로 사용
                if (Files.exists(dbPath)) {
                    log.debug("원본 파일이 이미 존재합니다. 파일: {}", dbPath);
                    return dbPath;
                }
                
                // 압축 해제 실행
                var compression = compressionFactory.resolve();
                var compressedData = Files.readAllBytes(compressedPath);
                var originalData = compression.decompress(compressedData);
                
                // 압축 해제된 파일 저장
                createDirectoryIfNotExists(dbPath.getParent());
                Files.write(dbPath, originalData);
                
                log.info("SQLite 파일 압축 해제 완료. 압축: {}bytes, 원본: {}bytes, 파일: {}", 
                        compressedData.length, originalData.length, dbPath);
                        
                return dbPath;
            }
            
            // 압축된 파일도 원본 파일도 없는 경우
            return dbPath;
            
        } catch (Exception e) {
            log.error("SQLite 파일 압축 해제 실패. 파일: {}", dbPath, e);
            return dbPath; // 실패해도 원본 경로 반환
        }
    }
    
    /**
     * 압축된 파일 경로 생성
     */
    private Path getCompressedPath(Path originalPath) {
        var compressionType = policyProvider.getCurrentPolicy()
            .getArchivingStrategy()
            .getCompressionType();
        
        return Paths.get(originalPath.toString() + compressionType.getExtension());
    }
}
