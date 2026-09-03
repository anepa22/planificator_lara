package ar.com.anepanet.planificator.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Task(
        UUID id,
        String title,
        String description,
        String status,
        String blockReason,
        String locationId,
        String locationName,
        String locationColor,
        UUID assigneePersonId,
        String assigneeName,
        String assigneeColor,
        boolean onBoard,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
