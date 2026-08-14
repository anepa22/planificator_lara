package ar.com.anepanet.planificator.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Person(
        UUID id,
        String name,
        String color,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
