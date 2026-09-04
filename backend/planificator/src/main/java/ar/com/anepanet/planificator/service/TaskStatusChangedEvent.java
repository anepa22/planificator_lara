package ar.com.anepanet.planificator.service;

public record TaskStatusChangedEvent(
        String chatId,
        String title,
        String fromStatus,
        String toStatus,
        String assigneeName,
        String locationName,
        String blockReason,
        String changedBy
) {}
