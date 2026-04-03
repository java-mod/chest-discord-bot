package com.example.chestbot.service;

import com.example.chestbot.util.TextSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@Order(0)
public class LegacyH2MigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyH2MigrationRunner.class);
    private static final String MIGRATION_KEY = "legacy_h2_to_mysql_v1";
    private static final String LEGACY_USERNAME = "sa";
    private static final String LEGACY_PASSWORD = "";
    private static final List<String> TABLE_ORDER = List.of(
            "guilds",
            "islands",
            "island_channels",
            "chest_config_versions",
            "chest_definitions",
            "admin_codes",
            "chest_log_events",
            "island_bank_log_events"
    );

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.legacy-h2.enabled:true}")
    private boolean enabled;

    @Value("${app.legacy-h2.search-paths:/root/bot/chest_bot/data/chestbot,./data/chestbot}")
    private String searchPaths;

    @Value("${app.legacy-h2.primary-username:chest_bot}")
    private String primaryUsername;

    @Value("${app.legacy-h2.primary-password:}")
    private String primaryPassword;

    public LegacyH2MigrationRunner(DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        createMarkerTableIfNeeded();
        if (isMigrationDone()) {
            return;
        }

        Optional<String> sourceUrl = resolveLegacyH2Url();
        if (sourceUrl.isEmpty()) {
            log.info("레거시 H2 파일을 찾지 못해 H2 → MySQL 마이그레이션을 건너뜁니다");
            return;
        }

        if (hasExistingApplicationData()) {
            log.warn("MySQL 대상 DB에 이미 데이터가 있어 레거시 H2 마이그레이션을 건너뜁니다");
            return;
        }

        try (Connection source = openLegacyConnection(sourceUrl.get())) {
            log.info("레거시 H2 → MySQL 마이그레이션 시작: {}", sourceUrl.get());
            upsertMigrationState("RUNNING", sourceUrl.get());

            transactionTemplate.executeWithoutResult(status -> migrateAllTables(source));

            upsertMigrationState("DONE", sourceUrl.get());
            log.info("레거시 H2 → MySQL 마이그레이션 완료");
        } catch (Exception e) {
            upsertMigrationState("FAILED", e.getMessage());
            throw new IllegalStateException("레거시 H2 → MySQL 마이그레이션 실패: " + e.getMessage(), e);
        }
    }

    private void migrateAllTables(Connection source) {
        Connection target = DataSourceUtils.getConnection(jdbcTemplate.getDataSource());
        try {
            for (String tableName : TABLE_ORDER) {
                if (!sourceTableExists(source, tableName)) {
                    log.info("레거시 H2 테이블 없음 → 스킵: {}", tableName);
                    continue;
                }
                copyTable(source, target, tableName);
                resetAutoIncrementIfNeeded(tableName);
            }
        } finally {
            DataSourceUtils.releaseConnection(target, jdbcTemplate.getDataSource());
        }
    }

    private void copyTable(Connection source, Connection target, String tableName) {
        String selectSql = "SELECT * FROM " + tableName + " ORDER BY id ASC";
        try (Statement sourceStmt = source.createStatement();
             ResultSet rs = sourceStmt.executeQuery(selectSql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            if (columnCount == 0) {
                return;
            }

            String insertSql = buildInsertSql(tableName, meta, columnCount);
            try (PreparedStatement targetStmt = target.prepareStatement(insertSql)) {
                int migrated = 0;
                while (rs.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        targetStmt.setObject(i, sanitizeValue(rs.getObject(i)));
                    }
                    targetStmt.addBatch();
                    migrated++;
                }
                targetStmt.executeBatch();
                log.info("레거시 H2 테이블 복사 완료: {} ({}건)", tableName, migrated);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("테이블 복사 실패 [" + tableName + "]: " + e.getMessage(), e);
        }
    }

    private boolean sourceTableExists(Connection source, String tableName) {
        try (PreparedStatement stmt = source.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?"
        )) {
            stmt.setString(1, tableName.toUpperCase(Locale.ROOT));
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private void resetAutoIncrementIfNeeded(String tableName) {
        Long nextId = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(id), 0) + 1 FROM " + tableName,
                Long.class
        );
        if (nextId != null) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " AUTO_INCREMENT = " + nextId);
        }
    }

    private Optional<String> resolveLegacyH2Url() {
        List<String> candidates = new ArrayList<>();
        for (String raw : searchPaths.split(",")) {
            String trimmed = raw.trim();
            if (!trimmed.isEmpty()) {
                candidates.add(trimmed);
            }
        }

        for (String basePath : candidates) {
            if (Files.exists(Path.of(basePath + ".mv.db"))) {
                return Optional.of("jdbc:h2:file:" + basePath);
            }
        }
        return Optional.empty();
    }

    private Connection openLegacyConnection(String sourceUrl) throws SQLException {
        List<String[]> attempts = Arrays.asList(
                new String[]{primaryUsername, primaryPassword},
                new String[]{LEGACY_USERNAME, LEGACY_PASSWORD}
        );

        SQLException last = null;
        for (String[] attempt : attempts) {
            try {
                return DriverManager.getConnection(sourceUrl, attempt[0], attempt[1]);
            } catch (SQLException e) {
                last = e;
            }
        }
        throw last != null ? last : new SQLException("레거시 H2 접속 시도에 모두 실패했습니다");
    }

    private boolean hasExistingApplicationData() {
        return tableHasRows("guilds")
                || tableHasRows("islands")
                || tableHasRows("island_channels")
                || tableHasRows("chest_config_versions")
                || tableHasRows("chest_definitions")
                || tableHasRows("admin_codes")
                || tableHasRows("chest_log_events")
                || tableHasRows("island_bank_log_events");
    }

    private boolean tableHasRows(String tableName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
        return count != null && count > 0;
    }

    private void createMarkerTableIfNeeded() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS startup_migrations (
                    migration_key VARCHAR(64) PRIMARY KEY,
                    status VARCHAR(16) NOT NULL,
                    details VARCHAR(500) NULL,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """);
    }

    private boolean isMigrationDone() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM startup_migrations WHERE migration_key = ? AND status = 'DONE'",
                Integer.class,
                MIGRATION_KEY
        );
        return count != null && count > 0;
    }

    private void upsertMigrationState(String status, String details) {
        jdbcTemplate.update(
                """
                INSERT INTO startup_migrations (migration_key, status, details)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), details = VALUES(details)
                """,
                MIGRATION_KEY,
                status,
                truncate(details)
        );
    }

    private String buildInsertSql(String tableName, ResultSetMetaData meta, int columnCount) throws SQLException {
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) {
                columns.append(", ");
                values.append(", ");
            }
            columns.append(meta.getColumnName(i));
            values.append("?");
        }
        return "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ")";
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }

    private Object sanitizeValue(Object value) {
        if (value instanceof String text) {
            return TextSanitizer.stripEmoji(text);
        }
        return value;
    }
}
