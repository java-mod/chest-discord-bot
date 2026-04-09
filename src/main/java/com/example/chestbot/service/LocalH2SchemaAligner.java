package com.example.chestbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
@Order(0)
public class LocalH2SchemaAligner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalH2SchemaAligner.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public LocalH2SchemaAligner(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            if (url == null || !url.startsWith("jdbc:h2:")) {
                return;
            }
        }

        if (!columnExists("CHEST_LOG_EVENTS", "BINDING_ID")) {
            return;
        }

        jdbcTemplate.execute("ALTER TABLE chest_log_events DROP COLUMN binding_id");
        log.info("로컬 H2 스키마 정리: chest_log_events.binding_id 제거 완료");
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }
}
