package com.healthcare.appointmentservice.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentSchemaFixer {

    private static final Logger log = LoggerFactory.getLogger(AppointmentSchemaFixer.class);

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void fixSchema() {
        dropLegacyUniqueIndexIfPresent();
        addPrescriptionColumnsIfAbsent();
    }

    private void dropLegacyUniqueIndexIfPresent() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) " +
                    "FROM information_schema.statistics " +
                    "WHERE table_schema = DATABASE() " +
                    "  AND table_name = 'appoinment' " +
                    "  AND index_name = 'uk_doctor_date_starttime'",
                    Integer.class
            );
            if (count != null && count > 0) {
                jdbcTemplate.execute("ALTER TABLE appoinment DROP INDEX uk_doctor_date_starttime");
                log.info("Dropped legacy unique index uk_doctor_date_starttime on appoinment.");
            }
        } catch (Exception ex) {
            log.warn("Could not drop legacy unique index uk_doctor_date_starttime: {}", ex.getMessage());
        }
    }

    private void addPrescriptionColumnsIfAbsent() {
        addColumnIfAbsent("prescription_name", "ALTER TABLE appoinment ADD COLUMN prescription_name VARCHAR(255)");
        addColumnIfAbsent("prescription_data", "ALTER TABLE appoinment ADD COLUMN prescription_data LONGTEXT");
    }

    private void addColumnIfAbsent(String columnName, String alterSql) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) " +
                    "FROM information_schema.columns " +
                    "WHERE table_schema = DATABASE() " +
                    "  AND table_name   = 'appoinment' " +
                    "  AND column_name  = ?",
                    Integer.class,
                    columnName
            );
            if (count == null || count == 0) {
                jdbcTemplate.execute(alterSql);
                log.info("Added column '{}' to appoinment table.", columnName);
            }
        } catch (Exception ex) {
            log.warn("Could not add column '{}': {}", columnName, ex.getMessage());
        }
    }
}
