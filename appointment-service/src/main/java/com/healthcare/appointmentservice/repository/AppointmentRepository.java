package com.healthcare.appointmentservice.repository;

import com.healthcare.appointmentservice.entity.Appointment;
import com.healthcare.appointmentservice.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientId(Long patientId);

    List<Appointment> findByPatientEmailIgnoreCase(String patientEmail);

    List<Appointment> findByDoctorId(Long doctorId);

    List<Appointment> findByStatus(AppointmentStatus status);

    List<Appointment> findByDoctorIdAndAppointmentDate(Long doctorId, LocalDate appointmentDate);

    List<Appointment> findByDoctorIdAndAppointmentDateBetween(
            Long doctorId,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<Appointment> findByDoctorIdAndAppointmentDateAndStartTime(
            Long doctorId,
            LocalDate appointmentDate,
            LocalTime startTime
    );

    List<Appointment> findAllByDoctorIdAndAppointmentDateAndStartTime(
            Long doctorId,
            LocalDate appointmentDate,
            LocalTime startTime
    );

    long countByDoctorIdAndAppointmentDateAndStartTimeAndStatusIn(
            Long doctorId,
            LocalDate appointmentDate,
            LocalTime startTime,
            List<AppointmentStatus> statuses
    );

    long countByDoctorIdAndAppointmentDateAndStartTimeAndStatusInAndIdNot(
            Long doctorId,
            LocalDate appointmentDate,
            LocalTime startTime,
            List<AppointmentStatus> statuses,
            Long id
    );
}
