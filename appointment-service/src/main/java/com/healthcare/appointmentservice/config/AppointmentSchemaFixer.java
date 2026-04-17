package com.healthcare.appointmentservice.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Removes legacy uniqueness constraint that prevented multi-patient slots.
 * Uses INFORMATION_SCHEMA to avoid failing on fresh databases.
 */
@Component
@RequiredArgsConstructor
public class AppointmentSchemaFixer {

    private static final Logger log = LoggerFactory.getLogger(AppointmentSchemaFixer.class);

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void dropLegacyUniqueIndexIfPresent() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(1)
                    FROM information_schema.statistics
                    WHERE table_schema = DATABASE()
                      AND table_name = 'appoinment'
                      AND index_name = 'uk_doctor_date_starttime'
                    """,
                    Integer.class
            );

            if (count != null && count > 0) {
                jdbcTemplate.execute("ALTER TABLE appoinment DROP INDEX uk_doctor_date_starttime");
                log.info("Dropped legacy unique index uk_doctor_date_starttime on appoinment.");
            }
        } catch (Exception ex) {
            // Best-effort: do not fail service startup if schema is already correct or DB permissions differ.
            log.warn("Could not drop legacy unique index uk_doctor_date_starttime: {}", ex.getMessage());
        }
    }
}

