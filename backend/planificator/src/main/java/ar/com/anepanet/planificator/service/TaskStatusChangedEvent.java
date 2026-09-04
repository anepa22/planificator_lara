package ar.com.anepanet.planificator.service;

import java.util.List;

public record TaskStatusChangedEvent(
        List<String> chatIds,
        String title,
        String fromStatus,
        String toStatus,
        String assigneeName,
        String locationName,
        String blockReason,
        String changedBy
) {}
