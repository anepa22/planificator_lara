package ar.com.anepanet.planificator.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CreateVacationsRequest(
        @NotEmpty List<UUID> userIds,
        @NotNull LocalDate dateFrom,
        @NotNull LocalDate dateTo,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        List<UUID> replaceConflictIds
) {}
