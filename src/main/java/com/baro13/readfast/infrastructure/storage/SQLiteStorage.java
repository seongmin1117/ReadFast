package com.baro13.readfast.infrastructure.storage;

import com.baro13.readfast.domain.AuthLog;
import com.baro13.readfast.infrastructure.batch.config.DataRetentionProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SQLiteStorage implements DataStorage {
    
    private final DataRetentionProperties properties;
    
    @Override
    public void store(List<AuthLog> authLogs, LocalDate date) {
        try {
            Path sqlitePath = getSQLitePath(date);
            createDirectoryIfNotExists(sqlitePath.getParent());
            
            try (Connection conn = getConnection(sqlitePath)) {
                createTableIfNotExists(conn);
                createIndexesIfNotExists(conn);
                insertAuthLogs(conn, authLogs);
            }
            
            log.info("{}개 레코드를 SQLite에 성공적으로 저장: {}", authLogs.size(), sqlitePath);
        } catch (SQLException | IOException e) {
            log.error("{}일자 SQLite 저장 실패", date, e);
            throw new RuntimeException("SQLite 저장 실패", e);
        }
    }
    
    @Override
    public List<AuthLog> retrieve(LocalDate date) {
        try {
            Path sqlitePath = getSQLitePath(date);
            if (!Files.exists(sqlitePath)) {
                return Collections.emptyList();
            }
            
            try (Connection conn = getConnection(sqlitePath)) {
                return selectAllAuthLogs(conn);
            }
        } catch (SQLException e) {
            log.error("{}일자 SQLite 조회 실패", date, e);
            throw new RuntimeException("SQLite 조회 실패", e);
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
        Path sqlitePath = getSQLitePath(date);
        return Files.exists(sqlitePath);
    }
    
    @Override
    public void delete(LocalDate date) {
        try {
            Path sqlitePath = getSQLitePath(date);
            if (Files.exists(sqlitePath)) {
                Files.delete(sqlitePath);
                log.info("SQLite 파일 삭제 성공: {}", sqlitePath);
            }
        } catch (IOException e) {
            log.error("{}일자 SQLite 파일 삭제 실패", date, e);
            throw new RuntimeException("SQLite 파일 삭제 실패", e);
        }
    }
    
    @Override
    public StorageType getStorageType() {
        return StorageType.SQLITE;
    }
    
    /**
     * 특정 사용자의 인증 로그 조회 (성능 최적화)
     */
    public List<AuthLog> retrieveByUserId(LocalDate date, String userId) {
        try {
            Path sqlitePath = getSQLitePath(date);
            if (!Files.exists(sqlitePath)) {
                return Collections.emptyList();
            }
            
            try (Connection conn = getConnection(sqlitePath)) {
                return selectAuthLogsByUserId(conn, userId);
            }
        } catch (SQLException e) {
            log.error("{}일자 사용자 {} SQLite 조회 실패", date, userId, e);
            throw new RuntimeException("SQLite 사용자 조회 실패", e);
        }
    }
    
    /**
     * 사용자별 인증 로그 개수 조회 (성능 최적화)
     */
    public long countByUserId(LocalDate date, String userId) {
        try {
            Path sqlitePath = getSQLitePath(date);
            if (!Files.exists(sqlitePath)) {
                return 0;
            }
            
            try (Connection conn = getConnection(sqlitePath)) {
                return countAuthLogsByUserId(conn, userId);
            }
        } catch (SQLException e) {
            log.error("{}일자 사용자 {} SQLite 카운트 실패", date, userId, e);
            throw new RuntimeException("SQLite 사용자 카운트 실패", e);
        }
    }
    
    /**
     * 압축 파일을 SQLite로 변환
     */
    public void convertFromCompressedFile(Path compressedFilePath, LocalDate date) {
        try {
            Path sqlitePath = getSQLitePath(date);
            createDirectoryIfNotExists(sqlitePath.getParent());
            
            try (Connection conn = getConnection(sqlitePath)) {
                createTableIfNotExists(conn);
                createIndexesIfNotExists(conn);
                
                // CSV 파일 임포트 (압축 해제 후)
                importFromCsvFile(conn, compressedFilePath);
            }
            
            log.info("압축 파일을 SQLite로 변환 완료: {} -> {}", compressedFilePath, sqlitePath);
        } catch (SQLException | IOException e) {
            log.error("압축 파일 SQLite 변환 실패: {}", compressedFilePath, e);
            throw new RuntimeException("압축 파일 SQLite 변환 실패", e);
        }
    }
    
    /**
     * 분석용 통합 SQLite 생성
     */
    public void createAnalyticsDatabase(LocalDate startDate, LocalDate endDate) {
        try {
            Path analyticsDbPath = getAnalyticsDbPath();
            createDirectoryIfNotExists(analyticsDbPath.getParent());
            
            try (Connection conn = getConnection(analyticsDbPath)) {
                createTableIfNotExists(conn);
                createIndexesIfNotExists(conn);
                
                // 기존 데이터 삭제
                clearTable(conn);
                
                // 날짜 범위의 모든 SQLite 파일을 통합
                startDate.datesUntil(endDate.plusDays(1))
                    .forEach(date -> {
                        Path dailyDbPath = getSQLitePath(date);
                        if (Files.exists(dailyDbPath)) {
                            attachAndCopyData(conn, dailyDbPath, date);
                        }
                    });
                
                // 분석 최적화를 위한 인덱스 재생성
                optimizeForAnalytics(conn);
            }
            
            log.info("분석용 통합 SQLite 생성 완료: {}", analyticsDbPath);
        } catch (SQLException | IOException e) {
            log.error("분석용 SQLite 생성 실패", e);
            throw new RuntimeException("분석용 SQLite 생성 실패", e);
        }
    }
    
    private Path getSQLitePath(LocalDate date) {
        String fileName = date.format(DateTimeFormatter.ofPattern(properties.getArchiveFileFormat())) + ".db";
        return Paths.get(properties.getArchiveBasePath(), "sqlite", fileName);
    }
    
    private Path getAnalyticsDbPath() {
        return Paths.get(properties.getArchiveBasePath(), "analytics.db");
    }
    
    private Connection getConnection(Path sqlitePath) throws SQLException {
        String url = "jdbc:sqlite:" + sqlitePath.toString();
        Connection conn = DriverManager.getConnection(url);
        
        // SQLite 성능 최적화 설정
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA synchronous = NORMAL");
            stmt.execute("PRAGMA cache_size = 10000");
            stmt.execute("PRAGMA temp_store = MEMORY");
            stmt.execute("PRAGMA mmap_size = 268435456"); // 256MB
        }
        
        return conn;
    }
    
    private void createDirectoryIfNotExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
            log.info("디렉토리 생성: {}", directory);
        }
    }
    
    private void createTableIfNotExists(Connection conn) throws SQLException {
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS auth_logs (
                id INTEGER PRIMARY KEY,
                date INTEGER NOT NULL,
                device TEXT,
                user_id TEXT NOT NULL,
                result TEXT,
                endpoint TEXT
            )
            """;
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
        }
    }
    
    private void createIndexesIfNotExists(Connection conn) throws SQLException {
        String[] indexes = {
            "CREATE INDEX IF NOT EXISTS idx_auth_logs_date ON auth_logs(date)",
            "CREATE INDEX IF NOT EXISTS idx_auth_logs_user_id ON auth_logs(user_id)",
            "CREATE INDEX IF NOT EXISTS idx_auth_logs_user_date ON auth_logs(user_id, date)",
            "CREATE INDEX IF NOT EXISTS idx_auth_logs_result ON auth_logs(result)",
            "CREATE INDEX IF NOT EXISTS idx_auth_logs_device ON auth_logs(device)"
        };
        
        try (Statement stmt = conn.createStatement()) {
            for (String indexSql : indexes) {
                stmt.execute(indexSql);
            }
        }
    }
    
    private void insertAuthLogs(Connection conn, List<AuthLog> authLogs) throws SQLException {
        String insertSql = """
            INSERT INTO auth_logs (id, date, device, user_id, result, endpoint)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        conn.setAutoCommit(false);
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            for (AuthLog authLog : authLogs) {
                pstmt.setLong(1, authLog.getId());
                pstmt.setLong(2, authLog.getDate().getEpochSecond());
                pstmt.setString(3, authLog.getDevice());
                pstmt.setString(4, authLog.getUserId());
                pstmt.setString(5, authLog.getResult());
                pstmt.setString(6, authLog.getEndpoint());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
    
    private List<AuthLog> selectAllAuthLogs(Connection conn) throws SQLException {
        String selectSql = "SELECT * FROM auth_logs ORDER BY date";
        return executeSelectQuery(conn, selectSql);
    }
    
    private List<AuthLog> selectAuthLogsByUserId(Connection conn, String userId) throws SQLException {
        String selectSql = "SELECT * FROM auth_logs WHERE user_id = ? ORDER BY date";
        List<AuthLog> authLogs = new ArrayList<>();
        
        try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    authLogs.add(createAuthLogFromResultSet(rs));
                }
            }
        }
        
        return authLogs;
    }
    
    private long countAuthLogsByUserId(Connection conn, String userId) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM auth_logs WHERE user_id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(countSql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }
    
    private List<AuthLog> executeSelectQuery(Connection conn, String sql) throws SQLException {
        List<AuthLog> authLogs = new ArrayList<>();
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                authLogs.add(createAuthLogFromResultSet(rs));
            }
        }
        
        return authLogs;
    }
    
    private AuthLog createAuthLogFromResultSet(ResultSet rs) throws SQLException {
        return AuthLog.builder()
            .id(rs.getLong("id"))
            .date(Instant.ofEpochSecond(rs.getLong("date")))
            .device(rs.getString("device"))
            .userId(rs.getString("user_id"))
            .result(rs.getString("result"))
            .endpoint(rs.getString("endpoint"))
            .build();
    }
    
    private void importFromCsvFile(Connection conn, Path csvFilePath) throws SQLException {
        // CSV 파일 읽기는 LocalFileStorage의 압축 해제 기능 활용
        // 실제 구현에서는 CSV 파싱 로직 추가
        log.info("CSV 파일 임포트 시작: {}", csvFilePath);
        // TODO: CSV 파일 파싱 및 데이터 삽입 로직 구현
    }
    
    private void clearTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM auth_logs");
            stmt.execute("VACUUM");
        }
    }
    
    private void attachAndCopyData(Connection conn, Path dailyDbPath, LocalDate date) {
        try {
            String attachSql = String.format("ATTACH DATABASE '%s' AS daily_db", dailyDbPath.toString());
            String copySql = "INSERT INTO auth_logs SELECT * FROM daily_db.auth_logs";
            String detachSql = "DETACH DATABASE daily_db";
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(attachSql);
                stmt.execute(copySql);
                stmt.execute(detachSql);
            }
            
            log.debug("일별 SQLite 데이터 통합 완료: {}", date);
        } catch (SQLException e) {
            log.error("일별 SQLite 데이터 통합 실패: {}", date, e);
        }
    }
    
    private void optimizeForAnalytics(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // 분석 최적화를 위한 추가 인덱스
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_analytics_date_result ON auth_logs(date, result)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_analytics_device_date ON auth_logs(device, date)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_analytics_endpoint_date ON auth_logs(endpoint, date)");
            
            // 통계 정보 업데이트
            stmt.execute("ANALYZE auth_logs");
        }
    }
}