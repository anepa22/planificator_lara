package ar.com.anepanet.planificator.domain;

import java.time.OffsetDateTime;

public record Location(
        String id,
        String name,
        String color,
        String colorSoft,
        short sortOrder,
        boolean active,
        boolean supportsVidriera,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
