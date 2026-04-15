package com.ayubo.queue_service.controller;

import com.ayubo.queue_service.dto.ApiResponse;
import com.ayubo.queue_service.dto.QueueCreateRequest;
import com.ayubo.queue_service.dto.QueueResponse;
import com.ayubo.queue_service.dto.QueueStatusUpdateRequest;
import com.ayubo.queue_service.service.QueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    @PostMapping("/entries")
    public ApiResponse<QueueResponse> createQueueEntry(@Valid @RequestBody QueueCreateRequest request) {
        return ApiResponse.<QueueResponse>builder()
                .success(true)
                .message("Queue entry created successfully")
                .data(queueService.createQueueEntry(request))
                .build();
    }

    @GetMapping("/my")
    public ApiResponse<List<QueueResponse>> getMyQueueEntries() {
        return ApiResponse.<List<QueueResponse>>builder()
                .success(true)
                .message("Your queue entries fetched successfully")
                .data(queueService.getMyQueueEntries())
                .build();
    }

    @GetMapping("/doctor/my")
    public ApiResponse<List<QueueResponse>> getDoctorQueue(
            @RequestParam(value = "date", required = false) LocalDate date
    ) {
        return ApiResponse.<List<QueueResponse>>builder()
                .success(true)
                .message("Doctor queue fetched successfully")
                .data(queueService.getDoctorQueue(date))
                .build();
    }

    @GetMapping("/admin/all")
    public ApiResponse<List<QueueResponse>> getAllQueueEntries() {
        return ApiResponse.<List<QueueResponse>>builder()
                .success(true)
                .message("All queue entries fetched successfully")
                .data(queueService.getAllQueueEntries())
                .build();
    }

    @PatchMapping("/entries/{id}/status")
    public ApiResponse<QueueResponse> updateQueueStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody QueueStatusUpdateRequest request
    ) {
        return ApiResponse.<QueueResponse>builder()
                .success(true)
                .message("Queue status updated successfully")
                .data(queueService.updateQueueStatus(id, request))
                .build();
    }
}
