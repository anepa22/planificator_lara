package ar.com.anepanet.planificator.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TaskHistory(
        long id,
        UUID taskId,
        UUID actorUserId,
        String actorName,
        String action,
        String fromStatus,
        String toStatus,
        UUID fromAssigneeUserId,
        String fromAssigneeName,
        UUID toAssigneeUserId,
        String toAssigneeName,
        String blockReason,
        OffsetDateTime occurredAt
) {}
