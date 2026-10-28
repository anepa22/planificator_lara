package ar.com.anepanet.planificator.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SaveVacationAdjustmentRequest(
        @NotNull UUID userId,
        @NotNull Integer periodYear,
        @NotNull Integer days,
        Integer carried
) {}
