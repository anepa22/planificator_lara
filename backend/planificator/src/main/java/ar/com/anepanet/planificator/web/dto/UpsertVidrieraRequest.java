package ar.com.anepanet.planificator.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpsertVidrieraRequest(
        @NotNull String locationId,
        @NotNull LocalDate workDate
) {}
