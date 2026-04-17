package com.ayubo.payment_service.repository;

import com.ayubo.payment_service.entity.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {

    TransactionRecord findByStripeSessionId(String stripeSessionId);

    TransactionRecord findTopByAppointmentIdOrderByCreatedAtDesc(Long appointmentId);
}