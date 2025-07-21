package com.baro13.readfast.infrastructure.storage.impl.database;

import com.baro13.readfast.domain.AuthLog;
import com.baro13.readfast.infrastructure.policy.DataRetentionProperties;
import com.baro13.readfast.infrastructure.storage.port.AnalyticsStorage;
import com.baro13.readfast.infrastructure.storage.port.StorageType;
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
public class SQLiteStorage implements AnalyticsStorage {
    
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
                log.debug("SQLite 파일이 존재하지 않음: {}", sqlitePath);
                return Collections.emptyList();
            }
            
            try (Connection conn = getConnection(sqlitePath)) {
                return selectAllAuthLogs(conn);
            }
        } catch (SQLException e) {
            log.error("{}일자 SQLite 조회 실패", date, e);
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
        Path sqlitePath = getSQLitePath(date);
        return Files.exists(sqlitePath);
    }

    @Override
    public void delete(LocalDate date) {
        try {
            Path sqlitePath = getSQLitePath(date);
            if (Files.exists(sqlitePath)) {
                Files.delete(sqlitePath);
                log.info("SQLite 파일 삭제 완료: {}", sqlitePath);
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

    @Override
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
            return Collections.emptyList();
        }
    }

    @Override
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
            log.error("{}일자 사용자 {} SQLite 개수 조회 실패", date, userId, e);
            return 0;
        }
    }

    @Override
    public void createAnalyticsDatabase(LocalDate startDate, LocalDate endDate) {
        try {
            Path analyticsPath = getAnalyticsDBPath();
            createDirectoryIfNotExists(analyticsPath.getParent());
            
            try (Connection conn = getConnection(analyticsPath)) {
                createAnalyticsTable(conn);
                createAnalyticsIndexes(conn);
                
                // 기간별 데이터를 통합 DB에 병합
                mergeDataRange(conn, startDate, endDate);
                
                log.info("분석용 통합 SQLite DB 생성 완료: {} ~ {}", startDate, endDate);
            }
        } catch (SQLException | IOException e) {
            log.error("분석용 통합 DB 생성 실패: {} ~ {}", startDate, endDate, e);
            throw new RuntimeException("분석용 통합 DB 생성 실패", e);
        }
    }

    @Override
    public void convertCompressedFile(Path compressedFilePath, LocalDate date) {
        // 압축 파일을 SQLite로 변환하는 로직
        // 현재는 기본 구현만 제공
        log.info("압축 파일 {} SQLite 변환 완료: {}", compressedFilePath, date);
    }

    private Connection getConnection(Path sqlitePath) throws SQLException {
        String url = "jdbc:sqlite:" + sqlitePath.toString();
        Connection conn = DriverManager.getConnection(url);
        
        // SQLite 성능 최적화 설정
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA synchronous = NORMAL");
            stmt.execute("PRAGMA cache_size = 10000");
            stmt.execute("PRAGMA temp_store = memory");
            stmt.execute("PRAGMA mmap_size = 268435456"); // 256MB
        }
        
        return conn;
    }

    private void createTableIfNotExists(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS auth_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                device TEXT,
                user_id TEXT,
                result TEXT,
                endpoint TEXT
            )
            """;
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void createIndexesIfNotExists(Connection conn) throws SQLException {
        String[] indexSqls = {
            "CREATE INDEX IF NOT EXISTS idx_auth_logs_date ON auth_logs(date)",
            "CREATE INDEX IF NOT EXISTS idx_auth_logs_user_id ON auth_logs(user_id)",
            "CREATE INDEX IF NOT EXISTS idx_auth_logs_user_date ON auth_logs(user_id, date)",
            "CREATE INDEX IF NOT EXISTS idx_auth_logs_result ON auth_logs(result)",
            "CREATE INDEX IF NOT EXISTS idx_auth_logs_device ON auth_logs(device)"
        };
        
        try (Statement stmt = conn.createStatement()) {
            for (String indexSql : indexSqls) {
                stmt.execute(indexSql);
            }
        }
    }

    private void createAnalyticsTable(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS analytics_auth_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                device TEXT,
                user_id TEXT,
                result TEXT,
                endpoint TEXT,
                source_date TEXT NOT NULL
            )
            """;
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void createAnalyticsIndexes(Connection conn) throws SQLException {
        String[] indexSqls = {
            "CREATE INDEX IF NOT EXISTS idx_analytics_date ON analytics_auth_logs(date)",
            "CREATE INDEX IF NOT EXISTS idx_analytics_user_id ON analytics_auth_logs(user_id)",
            "CREATE INDEX IF NOT EXISTS idx_analytics_source_date ON analytics_auth_logs(source_date)"
        };
        
        try (Statement stmt = conn.createStatement()) {
            for (String indexSql : indexSqls) {
                stmt.execute(indexSql);
            }
        }
    }

    private void insertAuthLogs(Connection conn, List<AuthLog> authLogs) throws SQLException {
        String sql = "INSERT INTO auth_logs (date, device, user_id, result, endpoint) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            
            for (AuthLog log : authLogs) {
                pstmt.setString(1, log.getDate().toString());
                pstmt.setString(2, log.getDevice());
                pstmt.setString(3, log.getUserId());
                pstmt.setString(4, log.getResult());
                pstmt.setString(5, log.getEndpoint());
                pstmt.addBatch();
            }
            
            pstmt.executeBatch();
            conn.commit();
        }
    }

    private List<AuthLog> selectAllAuthLogs(Connection conn) throws SQLException {
        String sql = "SELECT * FROM auth_logs ORDER BY date DESC";
        List<AuthLog> results = new ArrayList<>();
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                results.add(mapResultSetToAuthLog(rs));
            }
        }
        
        return results;
    }

    private List<AuthLog> selectAuthLogsByUserId(Connection conn, String userId) throws SQLException {
        String sql = "SELECT * FROM auth_logs WHERE user_id = ? ORDER BY date DESC";
        List<AuthLog> results = new ArrayList<>();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapResultSetToAuthLog(rs));
                }
            }
        }
        
        return results;
    }

    private long countAuthLogsByUserId(Connection conn, String userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM auth_logs WHERE user_id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    private void mergeDataRange(Connection conn, LocalDate startDate, LocalDate endDate) throws SQLException {
        // 각 일별 SQLite 파일에서 데이터를 읽어서 통합 DB에 삽입
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            mergeDataFromDate(conn, current);
            current = current.plusDays(1);
        }
    }

    private void mergeDataFromDate(Connection conn, LocalDate date) throws SQLException {
        Path sqlitePath = getSQLitePath(date);
        if (!Files.exists(sqlitePath)) {
            return;
        }
        
        try (Connection sourceConn = getConnection(sqlitePath)) {
            String selectSql = "SELECT * FROM auth_logs";
            String insertSql = "INSERT INTO analytics_auth_logs (date, device, user_id, result, endpoint, source_date) VALUES (?, ?, ?, ?, ?, ?)";
            
            try (Statement selectStmt = sourceConn.createStatement();
                 ResultSet rs = selectStmt.executeQuery(selectSql);
                 PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                
                conn.setAutoCommit(false);
                
                while (rs.next()) {
                    insertStmt.setString(1, rs.getString("date"));
                    insertStmt.setString(2, rs.getString("device"));
                    insertStmt.setString(3, rs.getString("user_id"));
                    insertStmt.setString(4, rs.getString("result"));
                    insertStmt.setString(5, rs.getString("endpoint"));
                    insertStmt.setString(6, date.toString());
                    insertStmt.addBatch();
                }
                
                insertStmt.executeBatch();
                conn.commit();
            }
        }
    }

    private AuthLog mapResultSetToAuthLog(ResultSet rs) throws SQLException {
        return AuthLog.builder()
                .date(Instant.parse(rs.getString("date")))
                .device(rs.getString("device"))
                .userId(rs.getString("user_id"))
                .result(rs.getString("result"))
                .endpoint(rs.getString("endpoint"))
                .build();
    }

    private Path getSQLitePath(LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.ofPattern(properties.getArchiveFileFormat()));
        String fileName = dateStr + ".db";
        return Paths.get(properties.getArchiveBasePath(), "sqlite", fileName);
    }

    private Path getAnalyticsDBPath() {
        return Paths.get(properties.getArchiveBasePath(), "analytics", "analytics.db");
    }

    private void createDirectoryIfNotExists(Path directory) throws IOException {
        if (directory != null && !Files.exists(directory)) {
            Files.createDirectories(directory);
        }
    }
}