-- Demo seed for your current local accounts shown in phpMyAdmin.
-- Safe approach:
-- 1. Open phpMyAdmin
-- 2. Run the appointment inserts on ayubo_appointment_db
-- 3. Run the telemedicine inserts on ayubo_telemedicine_db
--
-- Based on your screenshots:
--   Patient user id/email  : 2 / wenurarcc@gmail.com
--   Provider doctor ids    : 3 / 4
--   Doctor slot owner id 3 : Dr. Wenux Kavin
--
-- Important note about slots:
--   doctor_schedule_slots belongs to the auth-service schedule feature.
--   The current appointment-service availability logic does not read that table yet.
--   It uses its own fixed time list plus already-booked appointments.


-- =========================================================
-- DATABASE: ayubo_appointment_db
-- TABLE   : appoinment
-- =========================================================

INSERT INTO `appoinment`
(`id`, `appointment_number`, `patient_id`, `patient_email`, `appointment_for`, `appointment_type`,
 `patient_title`, `patient_name`, `contact_number`, `identification_type`, `identification_value`,
 `contact_email`, `doctor_id`, `specialty`, `appointment_date`, `start_time`, `end_time`,
 `reason`, `note_or_address`, `no_show_refund`, `on_going_number`, `status`, `payment_status`,
 `meeting_link`, `notes`, `cancel_reason`, `reschedule_count`, `created_at`, `updated_at`)
VALUES
(1001, 'APT-DEMO1001', 2, 'wenurarcc@gmail.com', 'Self', 'VIDEO_CONSULTATION',
 'Mr', 'Wenura Kavinda', '0712345678', 'NIC', '200012345678',
 'wenurarcc@gmail.com', 3, 'Neurology', '2026-04-16', '10:00:00', '10:30:00',
 'Migraine follow-up', 'Online consultation', 0, 0, 'CONFIRMED', 'PAID',
 NULL, 'Demo confirmed appointment for telemedicine', NULL, 0, NOW(), NOW()),

(1002, 'APT-DEMO1002', 2, 'wenurarcc@gmail.com', 'Self', 'IN_PERSON',
 'Mr', 'Wenura Kavinda', '0712345678', 'NIC', '200012345678',
 'wenurarcc@gmail.com', 3, 'Neurology', '2026-04-17', '11:00:00', '11:30:00',
 'Routine neurological review', 'Bring previous reports', 0, 0, 'PENDING_PAYMENT', 'PENDING',
 NULL, 'Demo pending appointment', NULL, 0, NOW(), NOW()),

(1003, 'APT-DEMO1003', 2, 'wenurarcc@gmail.com', 'Self', 'VIDEO_CONSULTATION',
 'Mr', 'Wenura Kavinda', '0712345678', 'NIC', '200012345678',
 'wenurarcc@gmail.com', 4, 'General Practice', '2026-04-18', '14:00:00', '14:30:00',
 'General consultation', 'Demo appointment with second provider', 0, 0, 'COMPLETED', 'PAID',
 NULL, 'Completed demo appointment', NULL, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
`updated_at` = NOW(),
`patient_email` = VALUES(`patient_email`),
`contact_email` = VALUES(`contact_email`),
`status` = VALUES(`status`),
`payment_status` = VALUES(`payment_status`),
`notes` = VALUES(`notes`);


-- =========================================================
-- DATABASE: ayubo_telemedicine_db
-- TABLE   : telemedicine_sessions
-- =========================================================

INSERT INTO `telemedicine_sessions`
(`id`, `appointment_id`, `doctor_id`, `patient_id`, `queue_entry_id`, `patient_email`,
 `consultation_type`, `meeting_provider`, `room_name`, `doctor_join_url`, `patient_join_url`,
 `status`, `scheduled_at`, `started_at`, `ended_at`, `notes`, `created_at`, `updated_at`)
VALUES
(2001, 1001, 3, 2, NULL, 'wenurarcc@gmail.com',
 'VIDEO_CONSULTATION', 'JITSI', 'ayubo-demo-1001-3-a1b2c3d4',
 'https://meet.jit.si/ayubo-demo-1001-3-a1b2c3d4',
 'https://meet.jit.si/ayubo-demo-1001-3-a1b2c3d4',
 'SCHEDULED', '2026-04-16 10:00:00', NULL, NULL, 'Demo scheduled telemedicine session', NOW(), NOW()),

(2002, 1003, 4, 2, NULL, 'wenurarcc@gmail.com',
 'VIDEO_CONSULTATION', 'JITSI', 'ayubo-demo-1003-4-e5f6g7h8',
 'https://meet.jit.si/ayubo-demo-1003-4-e5f6g7h8',
 'https://meet.jit.si/ayubo-demo-1003-4-e5f6g7h8',
 'ENDED', '2026-04-18 14:00:00', '2026-04-18 14:02:00', '2026-04-18 14:18:00', 'Demo completed telemedicine session', NOW(), NOW())
ON DUPLICATE KEY UPDATE
`updated_at` = NOW(),
`status` = VALUES(`status`),
`notes` = VALUES(`notes`),
`patient_email` = VALUES(`patient_email`);


-- =========================================================
-- OPTIONAL CHECK QUERIES
-- =========================================================
-- SELECT * FROM ayubo_appointment_db.appoinment WHERE id IN (1001,1002,1003);
-- SELECT * FROM ayubo_telemedicine_db.telemedicine_sessions WHERE id IN (2001,2002);
