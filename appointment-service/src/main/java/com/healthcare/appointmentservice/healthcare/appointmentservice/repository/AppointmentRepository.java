package com.healthcare.appointmentservice.healthcare.appointmentservice.repository;

import com.healthcare.appointmentservice.healthcare.appointmentservice.entity.Appointment;
import com.healthcare.appointmentservice.healthcare.appointmentservice.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientId(Long patientId);

    List<Appointment> findByDoctorId(Long doctorId);

    List<Appointment> findByStatus(AppointmentStatus status);

    List<Appointment> findByDoctorIdAndAppointmentDate(Long doctorId, LocalDate appointmentDate);

    Optional<Appointment> findByDoctorIdAndAppointmentDateAndStartTime(
            Long doctorId,
            LocalDate appointmentDate,
            LocalTime startTime
    );
}