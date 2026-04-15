package com.ayubo.queue_service.service;

import com.ayubo.queue_service.dto.QueueCreateRequest;
import com.ayubo.queue_service.dto.QueueResponse;
import com.ayubo.queue_service.dto.QueueStatusUpdateRequest;

import java.time.LocalDate;
import java.util.List;

public interface QueueService {
    QueueResponse createQueueEntry(QueueCreateRequest request);

    List<QueueResponse> getMyQueueEntries();

    List<QueueResponse> getDoctorQueue(LocalDate date);

    List<QueueResponse> getAllQueueEntries();

    QueueResponse updateQueueStatus(Long id, QueueStatusUpdateRequest request);
}
