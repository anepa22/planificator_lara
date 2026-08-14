package ar.com.anepanet.planificator.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditEntry(
        UUID id,
        OffsetDateTime occurredAt,
        UUID userId,
        String username,
        String action,
        String entityType,
        String entityId,
        String summary
) {}
