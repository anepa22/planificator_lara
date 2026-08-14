package ar.com.anepanet.planificator.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Shift(
        UUID id,
        UUID personId,
        String locationId,
        LocalDate workDate,
        LocalTime startTime,
        LocalTime endTime,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
