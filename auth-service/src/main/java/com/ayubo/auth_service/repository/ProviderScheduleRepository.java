package com.ayubo.auth_service.repository;

import com.ayubo.auth_service.model.ProviderSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProviderScheduleRepository extends JpaRepository<ProviderSchedule, Long> {

    List<ProviderSchedule> findByProviderId(Long providerId);

    @Query("""
            SELECT s FROM ProviderSchedule s
            WHERE s.provider.id = :providerId
              AND (s.slotDate = :day OR s.date = :dateStr)
            ORDER BY s.startTime ASC
            """)
    List<ProviderSchedule> findPublicSlotsForDay(
            @Param("providerId") Long providerId,
            @Param("day") LocalDate day,
            @Param("dateStr") String dateStr
    );
}