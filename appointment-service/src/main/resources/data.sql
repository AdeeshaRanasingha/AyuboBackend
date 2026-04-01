INSERT INTO appointments
(appointment_number, patient_id, doctor_id, specialty, appointment_date, start_time, end_time, reason, status, payment_status, meeting_link, notes, cancel_reason, reschedule_count, created_at, updated_at)
VALUES
    ('APT-DEMO0001', 101, 501, 'Cardiology', '2026-04-10', '10:00:00', '10:30:00', 'Heart checkup', 'CONFIRMED', 'PAID', NULL, 'Demo appointment', NULL, 0, NOW(), NOW()),
    ('APT-DEMO0002', 102, 502, 'Dermatology', '2026-04-11', '11:00:00', '11:30:00', 'Skin consultation', 'PENDING_PAYMENT', 'PENDING', NULL, 'Waiting for payment', NULL, 0, NOW(), NOW());